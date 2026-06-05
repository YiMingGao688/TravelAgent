<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { createDebouncedLocationTips, queryRoutes } from '../../utils/api.js'

const startDebouncedTips = createDebouncedLocationTips()
const endDebouncedTips = createDebouncedLocationTips()

const props = defineProps({
    routeData: { type: Object, default: () => ({ from: '', to: '', preference: '', tripPlanId: null, importSource: '' }) },
    tripSegments: { type: Array, default: () => [] },
    weatherInfo: { type: Object, default: null },
    geoInfo: { type: Object, default: null }
})

const emit = defineEmits(['update:routeData', 'booking', 'routeGenerated'])

// ---- 基础状态 ----
const startPoint = ref(props.routeData.from || '')
const endPoint = ref(props.routeData.to || '')
const preference = ref(props.routeData.preference || '舒适优先')
const selectedMode = ref('SMART')
const loading = ref(false)
const statusMessage = ref('')
const routeResults = ref([])
const activeRoute = ref(null)
const startLocation = ref(null)
const endLocation = ref(null)
const startWeather = ref(null)
const endWeather = ref(null)
const serviceCapabilities = ref([])
const mapReady = ref(false)
const mapError = ref('')
const startSuggestions = ref([])
const endSuggestions = ref([])
const selectedStartPlace = ref(null)
const selectedEndPlace = ref(null)
const visPanel = ref('overview')

// ---- ECharts 实例 ----
let tempGaugeChart = null
let humidityGaugeChart = null
let timelineChart = null

// ---- 高德地图对象 ----
let map = null
let driving = null
let geocoder = null
let autoComplete = null
let startMarker = null
let endMarker = null
let animatedMarker = null
let startSuggestTimer = null
let endSuggestTimer = null
let animTimer = null
let currentPolylines = []
let routeMarkers = []

// ---- 计算属性 ----
const canSearch = computed(() => Boolean(startPoint.value.trim() && endPoint.value.trim()))
const hasSegments = computed(() => props.tripSegments && props.tripSegments.length > 0)
const imported = computed(() => Boolean(props.routeData.importSource))

const routeTitle = computed(() => {
    if (hasSegments.value) return `多段行程 (${props.tripSegments.length} 段)`
    if (!startPoint.value && !endPoint.value) return '选择地点生成路线'
    return `${startPoint.value || '出发地'} → ${endPoint.value || '目的地'}`
})

const routeSummary = computed(() => {
    if (!activeRoute.value) return null
    return {
        distance: activeRoute.value.distance || 0,
        duration: activeRoute.value.duration || 0,
        cost: activeRoute.value.cost || 0,
        reason: activeRoute.value.recommendReason || '暂无推荐理由'
    }
})

// 多段行程汇总
const segmentsSummary = computed(() => {
    if (!hasSegments.value) return null
    const totalDist = props.tripSegments.reduce((s, seg) => s + (seg.distanceKm || 0), 0)
    const totalDur = props.tripSegments.reduce((s, seg) => s + (seg.durationMinutes || 0), 0)
    const totalCost = props.tripSegments.reduce((s, seg) => s + (seg.estimatedCost || 0), 0)
    return { distance: Math.round(totalDist * 10) / 10, duration: totalDur, cost: Math.round(totalCost) }
})

// 天气可视化数据
const weatherChartData = computed(() => {
    const w = props.weatherInfo || endWeather.value
    if (!w) return null
    return {
        temperature: parseFloat(w.temperature) || 0,
        humidity: parseFloat(w.humidity) || 0,
        weather: w.weather || '未知',
        wind: `${w.winddirection || ''} ${w.windpower || ''}级`,
        city: w.city || ''
    }
})

const weatherAlerts = computed(() => [
    buildWeatherAlert('出发地', startWeather.value),
    buildWeatherAlert('目的地', endWeather.value || props.weatherInfo)
].filter(Boolean))

const risk = computed(() => buildRisk())

// ---- Watchers ----
watch(() => props.routeData, value => {
    startPoint.value = value?.from || ''
    endPoint.value = value?.to || ''
    preference.value = value?.preference || preference.value || '舒适优先'
    selectedStartPlace.value = null
    selectedEndPlace.value = null
    statusMessage.value = value?.importSource ? `已从 ${value.importSource} 导入` : ''
    if (startPoint.value && endPoint.value) nextTick(() => drawAmapRoute())
}, { deep: true, immediate: true })

watch(() => props.tripSegments, segs => {
    if (segs && segs.length) nextTick(() => drawMultiSegmentRoutes(segs))
}, { deep: true })

watch(() => props.weatherInfo, () => nextTick(renderWeatherCharts))
watch(visPanel, () => nextTick(renderWeatherCharts))

watch([startPoint, endPoint, preference], () => {
    emit('update:routeData', {
        ...props.routeData, from: startPoint.value, to: endPoint.value, preference: preference.value
    })
})

// ---- 地图初始化 ----
async function initMap() {
    if (map) return
    await nextTick()
    if (!window.AMap) { mapError.value = '地图服务暂不可用'; return }
    window.AMap.plugin(['AMap.Geocoder', 'AMap.Driving', 'AMap.Scale', 'AMap.ToolBar', 'AMap.AutoComplete'], () => {
        map = new window.AMap.Map('container', { zoom: 11, center: [118.796877, 32.060255], resizeEnable: true, viewMode: '2D' })
        map.addControl(new window.AMap.Scale())
        map.addControl(new window.AMap.ToolBar({ position: 'RB' }))
        geocoder = new window.AMap.Geocoder({ city: '全国' })
        autoComplete = new window.AMap.AutoComplete({ city: '全国', citylimit: false })
        driving = new window.AMap.Driving({ map, policy: window.AMap.DrivingPolicy.LEAST_TIME, autoFitView: true })
        mapReady.value = true
        if (hasSegments.value) drawMultiSegmentRoutes(props.tripSegments)
        else if (startPoint.value && endPoint.value) drawAmapRoute()
    })
}

function destroyMap() {
    if (startSuggestTimer) clearTimeout(startSuggestTimer)
    if (endSuggestTimer) clearTimeout(endSuggestTimer)
    if (animTimer) clearInterval(animTimer)
    destroyWeatherCharts()
    if (map) { map.destroy(); map = null; driving = null; geocoder = null; autoComplete = null; startMarker = null; endMarker = null; mapReady.value = false }
}

// ---- 多段行程绘制（★核心新增） ----
async function drawMultiSegmentRoutes(segments) {
    if (!map || !segments.length) return
    clearAllOverlays()

    const colors = ['#2563eb', '#0891b2', '#16a34a', '#f97316', '#7c3aed', '#db2777', '#ea580c', '#4f46e5']
    const allMarkers = []
    const addedPoints = new Set()

    for (let i = 0; i < segments.length; i++) {
        const seg = segments[i]
        const color = colors[i % colors.length]

        // 解析坐标
        const startLngLat = parseLocation(seg.startLocation)
        const endLngLat = parseLocation(seg.endLocation)
        if (!startLngLat || !endLngLat) continue

        // 添加节点标记（去重）
        const startKey = `${startLngLat[0]},${startLngLat[1]}`
        const endKey = `${endLngLat[0]},${endLngLat[1]}`

        if (!addedPoints.has(startKey)) {
            const label = i === 0 ? '起点' : `换乘${i}`
            const m = new window.AMap.Marker({
                position: startLngLat, map,
                label: { content: label, direction: 'top' },
                icon: new window.AMap.Icon({
                    size: new window.AMap.Size(20, 28),
                    image: `https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-${i === 0 ? 'red' : 'default'}.png`,
                    imageSize: new window.AMap.Size(20, 28)
                })
            })
            allMarkers.push(m)
            routeMarkers.push(m)
            addedPoints.add(startKey)
        }

        if (!addedPoints.has(endKey)) {
            const isLast = i === segments.length - 1
            const m = new window.AMap.Marker({
                position: endLngLat, map,
                label: { content: isLast ? '终点' : seg.endPlace?.substring(0, 6) || '', direction: 'bottom' },
                icon: new window.AMap.Icon({
                    size: new window.AMap.Size(20, 28),
                    image: `https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-${isLast ? 'blue' : 'default'}.png`,
                    imageSize: new window.AMap.Size(20, 28)
                })
            })
            allMarkers.push(m)
            routeMarkers.push(m)
            addedPoints.add(endKey)
        }

        // 绘制折线（带动画虚线）
        const polylinePath = seg.routePolyline
            ? decodePolyline(seg.routePolyline)
            : [startLngLat, endLngLat]

        const polyline = new window.AMap.Polyline({
            path: polylinePath,
            strokeColor: color,
            strokeWeight: 6,
            strokeOpacity: 0.85,
            lineJoin: 'round',
            lineCap: 'round',
            strokeStyle: 'solid',
            showDir: true,
            zIndex: 10
        })
        polyline.setMap(map)
        currentPolylines.push(polyline)

        // 添加段标签
        const midIdx = Math.floor(polylinePath.length / 2)
        const midPoint = polylinePath[midIdx]
        if (midPoint) {
            const segLabel = seg.transportMode === 'TRAIN' ? '🚄高铁' :
                seg.transportMode === 'TAXI' ? '🚕接驳' :
                seg.transportMode === 'DRIVE' ? '🚗自驾' : '📍转运'
            const textMarker = new window.AMap.Marker({
                position: midPoint, map,
                label: {
                    content: `<div style="background:${color};color:#fff;padding:2px 8px;border-radius:4px;font-size:11px;white-space:nowrap">${segLabel} ${seg.distanceKm || 0}km</div>`,
                    offset: new window.AMap.Pixel(0, -20)
                },
                icon: new window.AMap.Icon({ size: new window.AMap.Size(1, 1), image: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==' })
            })
            routeMarkers.push(textMarker)
        }
    }

    // 启动移动动画
    startRouteAnimation(segments)

    if (allMarkers.length) map.setFitView(allMarkers, false, [80, 80, 80, 120])
}

// 解码高德 polyline
function decodePolyline(polylineStr) {
    if (!polylineStr) return []
    const paths = []
    const segments = polylineStr.split(';').filter(Boolean)
    for (const seg of segments) {
        const coords = seg.split('|').map(p => {
            const parts = p.split(',').map(Number)
            return parts.length >= 2 && parts.every(isFinite) ? parts : null
        }).filter(Boolean)
        paths.push(...coords)
    }
    return paths.length ? paths : null
}

// 移动标记动画
function startRouteAnimation(segments) {
    if (animTimer) clearInterval(animTimer)
    if (!map || !segments.length) return

    // 构建完整路径
    const fullPath = []
    for (const seg of segments) {
        const polylineStr = seg.routePolyline
        if (polylineStr) {
            const decoded = decodePolyline(polylineStr)
            if (decoded) fullPath.push(...decoded)
        } else {
            const s = parseLocation(seg.startLocation)
            const e = parseLocation(seg.endLocation)
            if (s) fullPath.push(s)
            if (e) fullPath.push(e)
        }
    }
    if (!fullPath.length) return

    // 创建动画标记
    const carIcon = new window.AMap.Icon({
        size: new window.AMap.Size(26, 26),
        image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/car.png',
        imageSize: new window.AMap.Size(26, 26)
    })
    animatedMarker = new window.AMap.Marker({
        position: fullPath[0],
        icon: carIcon,
        anchor: 'center',
        zIndex: 100,
        map
    })

    let step = 0
    animTimer = setInterval(() => {
        step = (step + 1) % fullPath.length
        animatedMarker?.setPosition(fullPath[step])
    }, 60)

    // 60秒后停止
    setTimeout(() => { if (animTimer) { clearInterval(animTimer); animTimer = null } }, 60000)
}

// ---- ECharts 可视化（★核心新增） ----
function initWeatherCharts() {
    nextTick(() => renderWeatherCharts())
}

function renderWeatherCharts() {
    const data = weatherChartData.value
    if (!data) return

    // 温度仪表盘
    const tempDom = document.getElementById('temp-gauge')
    if (tempDom && !tempGaugeChart) {
        tempGaugeChart = echarts.init(tempDom)
    }
    if (tempGaugeChart) {
        tempGaugeChart.setOption({
            series: [{
                type: 'gauge',
                startAngle: 210, endAngle: -30,
                min: -20, max: 50,
                radius: '90%',
                center: ['50%', '58%'],
                axisLine: { lineStyle: { width: 14, color: [[0.3, '#67e0e3'], [0.6, '#37a2da'], [0.8, '#fd666d'], [1, '#e5323e']] } },
                pointer: { length: '60%', width: 6, itemStyle: { color: '#334155' } },
                detail: { valueAnimation: true, formatter: '{value}℃', fontSize: 16, color: '#0f172a', offsetCenter: [0, '70%'] },
                title: { offsetCenter: [0, '32%'], fontSize: 12, color: '#64748b' },
                data: [{ value: data.temperature, name: '温度' }]
            }],
            grid: { left: 0, right: 0, top: 0, bottom: 0 }
        }, true)
    }

    // 湿度仪表盘
    const humDom = document.getElementById('humidity-gauge')
    if (humDom && !humidityGaugeChart) {
        humidityGaugeChart = echarts.init(humDom)
    }
    if (humidityGaugeChart) {
        humidityGaugeChart.setOption({
            series: [{
                type: 'gauge',
                startAngle: 210, endAngle: -30,
                min: 0, max: 100,
                radius: '90%',
                center: ['50%', '58%'],
                axisLine: { lineStyle: { width: 14, color: [[0.3, '#a5d6a7'], [0.6, '#4caf50'], [0.8, '#ffa726'], [1, '#ef5350']] } },
                pointer: { length: '60%', width: 6, itemStyle: { color: '#334155' } },
                detail: { valueAnimation: true, formatter: '{value}%', fontSize: 16, color: '#0f172a', offsetCenter: [0, '70%'] },
                title: { offsetCenter: [0, '32%'], fontSize: 12, color: '#64748b' },
                data: [{ value: data.humidity, name: '湿度' }]
            }],
            grid: { left: 0, right: 0, top: 0, bottom: 0 }
        }, true)
    }

    // 行程时间轴
    const tlDom = document.getElementById('timeline-chart')
    if (tlDom && !timelineChart) {
        timelineChart = echarts.init(tlDom)
    }
    if (timelineChart && hasSegments.value) {
        const segs = props.tripSegments
        const names = segs.map(s => s.segmentType === 'INTERCITY' ? '🚄高铁' : s.segmentType === 'LOCAL_ACCESS' ? '🚕接驳' : s.segmentType === 'LOCAL_TRANSFER' ? '🚗转运' : s.segmentType === 'HOTEL_TRANSFER' ? '🏨酒店' : '📍' + (s.segmentType || ''))
        const durations = segs.map(s => s.durationMinutes || 0)
        const distances = segs.map(s => s.distanceKm || 0)

        timelineChart.setOption({
            tooltip: { trigger: 'axis' },
            grid: { left: 14, right: 14, top: 14, bottom: 14, containLabel: true },
            xAxis: { type: 'category', data: names, axisLabel: { fontSize: 10, rotate: 20 } },
            yAxis: [
                { type: 'value', name: '分钟', axisLabel: { fontSize: 10 } },
                { type: 'value', name: '公里', axisLabel: { fontSize: 10 } }
            ],
            series: [
                { name: '耗时', type: 'bar', data: durations, itemStyle: { borderRadius: [6, 6, 0, 0], color: '#2563eb' }, barWidth: 20 },
                { name: '距离', type: 'line', yAxisIndex: 1, data: distances, lineStyle: { color: '#f97316', width: 2 }, symbol: 'circle', symbolSize: 6, itemStyle: { color: '#f97316' } }
            ],
            color: ['#2563eb', '#f97316']
        }, true)
    }
}

function destroyWeatherCharts() {
    if (tempGaugeChart) { tempGaugeChart.dispose(); tempGaugeChart = null }
    if (humidityGaugeChart) { humidityGaugeChart.dispose(); humidityGaugeChart = null }
    if (timelineChart) { timelineChart.dispose(); timelineChart = null }
}

// ---- 原有单段路线绘制 ----
function onPlaceInput(type) {
    const keyword = type === 'start' ? startPoint.value.trim() : endPoint.value.trim()
    if (type === 'start') {
        selectedStartPlace.value = null
        if (keyword.length < 2) { startSuggestions.value = []; return }
        startSuggestions.value = []
        if (startSuggestTimer) clearTimeout(startSuggestTimer)
        startSuggestTimer = setTimeout(() => searchPlaces(keyword, 'start'), 500)
    } else {
        selectedEndPlace.value = null
        if (keyword.length < 2) { endSuggestions.value = []; return }
        endSuggestions.value = []
        if (endSuggestTimer) clearTimeout(endSuggestTimer)
        endSuggestTimer = setTimeout(() => searchPlaces(keyword, 'end'), 500)
    }
}

async function searchPlaces(keyword, type) {
    if (!keyword || keyword.length < 2) return
    try {
        const fn = type === 'start' ? startDebouncedTips : endDebouncedTips
        const tips = await fn(keyword)
        const places = tips.map(item => ({
            name: item.name, district: item.district || '', address: item.address || '',
            adcode: item.adcode || '', location: item.location
        }))
        if (type === 'start') startSuggestions.value = places
        else endSuggestions.value = places
    } catch { /* 静默处理 */ }
}

async function selectPlace(type, place) {
    if (type === 'start') {
        selectedStartPlace.value = place; startPoint.value = place.name
        startSuggestions.value = []; startLocation.value = toLocationInfo(place)
    } else {
        selectedEndPlace.value = place; endPoint.value = place.name
        endSuggestions.value = []; endLocation.value = toLocationInfo(place)
    }
    statusMessage.value = '地点已确认'
    if (canSearch.value) await drawAmapRoute()
    else previewSelectedPoints()
}

function toLocationInfo(place) {
    return { name: place.name, formattedAddress: [place.district, place.address, place.name].filter(Boolean).join(''), location: placeLocation(place), adcode: place.adcode, city: place.district }
}

function placeLocation(place) {
    if (!place?.location) return ''
    if (typeof place.location === 'string') return place.location
    if (place.location.lng !== undefined) return `${place.location.lng},${place.location.lat}`
    return ''
}

async function searchSmartRoute(showTip = true) {
    if (!canSearch.value) { if (showTip) mapError.value = '请填写出发地和目的地。'; return }
    if (!map) await initMap()
    loading.value = true; mapError.value = ''; statusMessage.value = '正在生成路线方案...'
    try {
        await drawAmapRoute()
        const userId = sessionStorage.getItem('loginUser') || 'guest'
        const plan = await queryRoutes({
            userId, tripPlanId: props.routeData.tripPlanId,
            startPoint: startPoint.value.trim(), endPoint: endPoint.value.trim(),
            startLocation: placeLocation(selectedStartPlace.value) || startLocation.value?.location || '',
            endLocation: placeLocation(selectedEndPlace.value) || endLocation.value?.location || '',
            startAdcode: selectedStartPlace.value?.adcode || startLocation.value?.adcode || '',
            endAdcode: selectedEndPlace.value?.adcode || endLocation.value?.adcode || '',
            startCity: selectedStartPlace.value?.district || startLocation.value?.city || '',
            endCity: selectedEndPlace.value?.district || endLocation.value?.city || '',
            routeType: selectedMode.value, preference: preference.value
        })
        startLocation.value = plan.startLocation || { name: startPoint.value }
        endLocation.value = plan.endLocation || { name: endPoint.value }
        startWeather.value = plan.startWeather || null
        endWeather.value = plan.endWeather || null
        serviceCapabilities.value = plan.serviceCapabilities || []
        routeResults.value = plan.routes || []
        activeRoute.value = routeResults.value.find(r => r.routeType === selectedMode.value) || routeResults.value[0] || null
        await drawAmapRoute()
        statusMessage.value = '路线方案已生成'
        nextTick(() => renderWeatherCharts())
        emit('routeGenerated', { ...plan, risk: risk.value, activeRoute: activeRoute.value })
    } catch (error) {
        mapError.value = friendlyError(error); statusMessage.value = '生成失败'
        await previewSelectedPoints()
    } finally { loading.value = false }
}

async function drawAmapRoute() {
    return new Promise(async resolve => {
        if (!window.AMap || !map || !driving || !canSearch.value) { resolve(); return }
        driving.clear()
        clearAllOverlays()
        const sl = await resolveLngLat('start'); const el = await resolveLngLat('end')
        if (!sl || !el) { mapError.value = '起点或终点还没有解析到坐标'; resolve(); return }
        startMarker = new window.AMap.Marker({ position: sl, map, label: { content: '起点', direction: 'top' } })
        endMarker = new window.AMap.Marker({ position: el, map, label: { content: '终点', direction: 'top' } })
        driving.search(sl, el, status => {
            if (status !== 'complete' && !mapError.value) mapError.value = '地图路线绘制失败'
            resolve()
        })
    })
}

function resolveLngLat(type) {
    const sel = type === 'start' ? selectedStartPlace.value : selectedEndPlace.value
    const cached = type === 'start' ? startLocation.value : endLocation.value
    const keyword = type === 'start' ? startPoint.value.trim() : endPoint.value.trim()
    const d = parseLocation(cached?.location || placeLocation(sel))
    if (d) return Promise.resolve(d)
    return geocodePoint(keyword, type)
}

function geocodePoint(keyword, type) {
    return new Promise(resolve => {
        if (!keyword || !geocoder) { resolve(null); return }
        geocoder.getLocation(keyword, (status, result) => {
            const first = status === 'complete' ? result?.geocodes?.[0] : null
            const ll = first?.location ? [first.location.lng, first.location.lat] : null
            if (ll) {
                const info = { name: keyword, formattedAddress: first.formattedAddress || keyword, location: `${first.location.lng},${first.location.lat}`, adcode: first.adcode || '', city: first.city || first.district || '' }
                if (type === 'start') { startLocation.value = info; selectedStartPlace.value = selectedStartPlace.value || info }
                else { endLocation.value = info; selectedEndPlace.value = selectedEndPlace.value || info }
            }
            resolve(ll)
        })
    })
}

function previewSelectedPoints() {
    if (!map) return
    clearAllOverlays()
    const sl = parseLocation(placeLocation(selectedStartPlace.value) || startLocation.value?.location)
    const el = parseLocation(placeLocation(selectedEndPlace.value) || endLocation.value?.location)
    const markers = []
    if (sl) { startMarker = new window.AMap.Marker({ position: sl, map, label: { content: '起点', direction: 'top' } }); markers.push(startMarker) }
    if (el) { endMarker = new window.AMap.Marker({ position: el, map, label: { content: '终点', direction: 'top' } }); markers.push(endMarker) }
    if (markers.length) map.setFitView(markers)
}

function clearAllOverlays() {
    if (startMarker) { map?.remove(startMarker); startMarker = null }
    if (endMarker) { map?.remove(endMarker); endMarker = null }
    if (animatedMarker) { map?.remove(animatedMarker); animatedMarker = null }
    currentPolylines.forEach(p => map?.remove(p))
    currentPolylines = []
    routeMarkers.forEach(m => map?.remove(m))
    routeMarkers = []
}

function parseLocation(location) {
    if (!location || !String(location).includes(',')) return null
    const [lng, lat] = String(location).split(',').map(Number)
    return Number.isNaN(lng) || Number.isNaN(lat) ? null : [lng, lat]
}

function swapPlaces() {
    const ot = startPoint.value, op = selectedStartPlace.value
    startPoint.value = endPoint.value; selectedStartPlace.value = selectedEndPlace.value
    endPoint.value = ot; selectedEndPlace.value = op
    startSuggestions.value = []; endSuggestions.value = []
    if (canSearch.value) drawAmapRoute()
}

async function applyRoute(route) {
    activeRoute.value = route; selectedMode.value = route.routeType || 'SMART'
    await drawAmapRoute()
    emit('routeGenerated', { startLocation: startLocation.value, endLocation: endLocation.value, startWeather: startWeather.value, endWeather: endWeather.value, routes: routeResults.value, risk: risk.value, activeRoute: activeRoute.value })
}

function weatherCity(w, fl) { return w?.city || fl?.city || fl?.district || fl?.name || '未知城市' }

function buildWeatherAlert(label, w) {
    if (!w) return null
    const t = `${w.weather || ''}${w.windpower || ''}`
    const temp = Number(w.temperature)
    if (t.includes('雨') || t.includes('雪') || t.includes('雾')) return `${label}有${w.weather}，建议预留进站和换乘时间。`
    if (!Number.isNaN(temp) && temp >= 32) return `${label}气温较高，建议室内候车补水。`
    if (!Number.isNaN(temp) && temp <= 3) return `${label}气温较低，关注道路结冰。`
    if (/[5-9]/.test(String(w.windpower || ''))) return `${label}风力偏大。`
    return `${label}天气适合出行。`
}

function buildRisk() {
    let score = 0; const reasons = []
    const d = Number(activeRoute.value?.distance || 0)
    const dur = Number(activeRoute.value?.duration || 0)
    const wt = `${startWeather.value?.weather || ''}${endWeather.value?.weather || ''}`
    if (/雨|雪|雾|雷/.test(wt)) { score += 2; reasons.push('天气可能影响出行体验') }
    if (d > 300 || dur > 240) { score += 2; reasons.push('跨城距离较长') }
    if (/老人|亲子/.test(preference.value)) { score += 1; reasons.push('偏好对舒适度要求较高') }
    const level = score >= 4 ? 'HIGH' : score >= 2 ? 'MEDIUM' : 'LOW'
    return { level, label: level === 'HIGH' ? '高风险' : level === 'MEDIUM' ? '中风险' : '低风险', reasons: reasons.length ? reasons : ['当前路线风险较低'] }
}

function friendlyError(e) {
    const m = e?.message || ''
    if (m.includes('AI') || m.includes('智谱')) return m
    if (m.includes('geocode') || m.includes('ENGINE')) return '地点解析失败，请从候选中选择具体地点。'
    return m || '路线规划失败'
}

defineExpose({ initMap, destroyMap, searchSmartRoute })
onMounted(() => { initMap(); initWeatherCharts() })
onBeforeUnmount(destroyMap)
</script>

<template>
    <div class="transit-module">
        <div class="map-shell">
            <div id="container" class="map-area"></div>
            <div v-if="mapError" class="map-error">{{ mapError }}</div>
        </div>

        <aside class="smart-panel">
            <div class="panel-scroll-area">
                <!-- 头部 -->
                <header class="smart-header">
                    <div>
                        <span class="eyebrow">{{ hasSegments ? '多段行程可视化' : '智能路线' }}</span>
                        <h2>{{ routeTitle }}</h2>
                    </div>
                    <span class="status-pill" :class="{ ready: mapReady }">{{ mapReady ? '地图已连接' : '加载中' }}</span>
                </header>

                <!-- 状态条 -->
                <div v-if="imported || statusMessage" class="import-banner" :class="{ success: statusMessage.includes('已') }">
                    <i class="fa-solid fa-circle-check"></i>
                    <span>{{ imported ? `已从 ${props.routeData.importSource} 导入` : statusMessage }}</span>
                </div>

                <!-- 多段行程面板切换 -->
                <div v-if="hasSegments" class="vis-tabs">
                    <button :class="{ active: visPanel === 'overview' }" @click="visPanel = 'overview'">📊 概览</button>
                    <button :class="{ active: visPanel === 'charts' }" @click="visPanel = 'charts'">📈 图表</button>
                    <button :class="{ active: visPanel === 'route' }" @click="visPanel = 'route'">🛣️ 路线</button>
                </div>

                <!-- ★ 可视化概览面板 -->
                <section v-if="hasSegments && visPanel === 'overview'" class="viz-overview">
                    <div v-if="segmentsSummary" class="metrics animate-in">
                        <div class="metric-card">
                            <i class="fa-solid fa-road"></i>
                            <strong>{{ segmentsSummary.distance }}</strong>
                            <span>公里</span>
                        </div>
                        <div class="metric-card">
                            <i class="fa-solid fa-clock"></i>
                            <strong>{{ segmentsSummary.duration }}</strong>
                            <span>分钟</span>
                        </div>
                        <div class="metric-card">
                            <i class="fa-solid fa-coins"></i>
                            <strong>{{ segmentsSummary.cost }}</strong>
                            <span>元预估</span>
                        </div>
                    </div>

                    <!-- ★ ECharts 仪表盘 -->
                    <div v-if="weatherChartData" class="gauge-row animate-in">
                        <div class="gauge-card">
                            <div class="gauge-label">🌡️ 目的地温度</div>
                            <div id="temp-gauge" class="gauge-dom"></div>
                        </div>
                        <div class="gauge-card">
                            <div class="gauge-label">💧 目的地湿度</div>
                            <div id="humidity-gauge" class="gauge-dom"></div>
                        </div>
                    </div>

                    <!-- 天气详情卡片 -->
                    <div v-if="weatherChartData" class="weather-detail-card animate-in">
                        <div class="weather-icon-large">{{ weatherIcon(weatherChartData.weather) }}</div>
                        <div class="weather-detail-body">
                            <strong>{{ weatherChartData.city || '目的地' }}</strong>
                            <div class="weather-detail-stats">
                                <span>🌡️ {{ weatherChartData.temperature }}℃</span>
                                <span>💧 {{ weatherChartData.humidity }}%</span>
                                <span>💨 {{ weatherChartData.wind }}</span>
                            </div>
                            <em>{{ weatherChartData.weather }}</em>
                        </div>
                    </div>

                    <!-- 段列表 -->
                    <div class="segment-list">
                        <article v-for="(seg, i) in props.tripSegments" :key="i" class="segment-card" :style="{ borderLeftColor: segColors[i % segColors.length] }">
                            <div class="seg-header">
                                <span class="seg-badge" :style="{ background: segColors[i % segColors.length] }">{{ segmentIcon(seg) }}</span>
                                <strong>{{ seg.title || (seg.startPlace + ' → ' + seg.endPlace) }}</strong>
                            </div>
                            <div class="seg-stats">
                                <span>{{ seg.transportMode || '转运' }}</span>
                                <span>{{ seg.durationMinutes || 0 }}分钟</span>
                                <span>{{ seg.distanceKm || 0 }}公里</span>
                                <span v-if="seg.estimatedCost">¥{{ seg.estimatedCost }}</span>
                            </div>
                        </article>
                    </div>
                </section>

                <!-- ★ ECharts 图表面板 -->
                <section v-if="hasSegments && visPanel === 'charts'" class="viz-charts">
                    <div class="chart-card animate-in">
                        <div class="chart-title">⏱️ 行程耗时与距离对比</div>
                        <div id="timeline-chart" class="chart-dom chart-timeline"></div>
                    </div>
                    <div v-if="weatherChartData" class="gauge-row animate-in">
                        <div class="gauge-card">
                            <div class="gauge-label">🌡️ 温度</div>
                            <div id="temp-gauge" class="gauge-dom"></div>
                        </div>
                        <div class="gauge-card">
                            <div class="gauge-label">💧 湿度</div>
                            <div id="humidity-gauge" class="gauge-dom"></div>
                        </div>
                    </div>
                </section>

                <!-- 路线输入（仅单段模式显示） -->
                <section v-if="!hasSegments" class="panel-card route-input">
                    <div class="section-title"><i class="fa-solid fa-location-dot"></i><span>路线输入</span></div>
                    <div class="field-wrap">
                        <div class="field-line"><span class="point start"></span><input v-model="startPoint" placeholder="搜索并选择出发地" @input="onPlaceInput('start')" /></div>
                        <div v-if="startSuggestions.length" class="suggest-panel">
                            <button v-for="item in startSuggestions" :key="item.name + item.location" @click="selectPlace('start', item)"><strong>{{ item.name }}</strong><span>{{ item.district }}{{ item.address }}</span></button>
                        </div>
                    </div>
                    <button class="swap-btn" @click="swapPlaces"><i class="fa-solid fa-right-left"></i></button>
                    <div class="field-wrap">
                        <div class="field-line"><span class="point end"></span><input v-model="endPoint" placeholder="搜索并选择目的地" @input="onPlaceInput('end')" /></div>
                        <div v-if="endSuggestions.length" class="suggest-panel">
                            <button v-for="item in endSuggestions" :key="item.name + item.location" @click="selectPlace('end', item)"><strong>{{ item.name }}</strong><span>{{ item.district }}{{ item.address }}</span></button>
                        </div>
                    </div>
                    <select v-model="preference"><option>舒适优先</option><option>少换乘</option><option>时间优先</option><option>经济优先</option><option>适合亲子/老人</option></select>
                    <div class="mode-tabs"><button :class="{ active: selectedMode === 'SMART' }" @click="selectedMode = 'SMART'">推荐</button><button :class="{ active: selectedMode === 'FAST' }" @click="selectedMode = 'FAST'">快速</button><button :class="{ active: selectedMode === 'ECO' }" @click="selectedMode = 'ECO'">经济</button></div>
                    <button class="primary-btn" :disabled="loading || !canSearch" @click="searchSmartRoute(true)"><i class="fa-solid fa-route"></i><span>{{ loading ? '正在生成...' : '生成路线方案' }}</span></button>
                </section>

                <!-- 路线摘要 -->
                <section v-if="!hasSegments && routeSummary" class="panel-card">
                    <div class="section-title"><i class="fa-solid fa-chart-simple"></i><span>路线摘要</span></div>
                    <div class="metrics"><div><strong>{{ routeSummary.distance }}</strong><span>公里</span></div><div><strong>{{ routeSummary.duration }}</strong><span>分钟</span></div><div><strong>{{ routeSummary.cost }}</strong><span>元预估</span></div></div>
                    <p class="reason">{{ activeRoute?.recommendReason }}</p>
                </section>

                <!-- 天气卡片（单段） -->
                <section v-if="!hasSegments" class="weather-section">
                    <article class="weather-card"><div class="weather-head"><span>出发地天气</span><strong>{{ weatherCity(startWeather, startLocation) }}</strong></div><template v-if="startWeather"><div class="temperature">{{ startWeather.temperature || '-' }}℃</div><div class="weather-main">{{ startWeather.weather || '-' }}</div><p>湿度 {{ startWeather.humidity || '-' }}% · {{ startWeather.winddirection || '-' }}风 {{ startWeather.windpower || '-' }}级</p><small>{{ startWeather.reporttime || '' }}</small></template><p v-else class="empty-line">暂无</p></article>
                    <article class="weather-card"><div class="weather-head"><span>目的地天气</span><strong>{{ weatherCity(endWeather, endLocation) }}</strong></div><template v-if="endWeather"><div class="temperature">{{ endWeather.temperature || '-' }}℃</div><div class="weather-main">{{ endWeather.weather || '-' }}</div><p>湿度 {{ endWeather.humidity || '-' }}% · {{ endWeather.winddirection || '-' }}风 {{ endWeather.windpower || '-' }}级</p><small>{{ endWeather.reporttime || '' }}</small></template><p v-else class="empty-line">暂无</p></article>
                </section>

                <!-- 风险 -->
                <section v-if="activeRoute" class="panel-card risk-card" :class="risk.level.toLowerCase()">
                    <div class="section-title"><i class="fa-solid fa-triangle-exclamation"></i><span>风险预警</span></div>
                    <div class="risk-level">{{ risk.label }} <small>{{ risk.level }}</small></div>
                    <p v-for="r in risk.reasons" :key="r">{{ r }}</p>
                    <p v-for="a in weatherAlerts" :key="a">{{ a }}</p>
                </section>

                <!-- 路线列表 -->
                <section v-if="routeResults.length" class="route-list">
                    <button v-for="route in routeResults" :key="route.id || route.routeType" :class="{ active: activeRoute?.routeType === route.routeType }" @click="applyRoute(route)">
                        <div><strong>{{ route.routeName }}</strong><span>{{ route.duration }}分钟 · {{ route.distance }}公里 · ¥{{ route.cost }}</span></div>
                        <i class="fa-solid fa-chevron-right"></i>
                    </button>
                </section>

                <!-- 操作 -->
                <section class="panel-card actions-card">
                    <div class="section-title"><i class="fa-solid fa-forward"></i><span>推荐操作</span></div>
                    <button class="secondary-btn" :disabled="!activeRoute && !hasSegments" @click="emit('booking')"><i class="fa-solid fa-route"></i><span>导入行程定制</span></button>
                </section>
            </div>
        </aside>
    </div>
</template>

<script>
// 工具函数（不参与响应式，放在普通 script 块中）
const segColors = ['#2563eb', '#0891b2', '#16a34a', '#f97316', '#7c3aed', '#db2777', '#ea580c', '#4f46e5']

function segmentIcon(seg) {
    const m = seg.transportMode || ''
    if (m === 'TRAIN') return '🚄'
    if (m === 'TAXI' || m === 'DRIVE') return '🚗'
    if (m === 'WALK') return '🚶'
    if (m === 'BUS') return '🚌'
    if (m === 'FLIGHT') return '✈️'
    return '📍'
}

function weatherIcon(w) {
    if (!w) return '☀️'
    if (w.includes('雨')) return '🌧️'
    if (w.includes('雪')) return '❄️'
    if (w.includes('云') || w.includes('阴')) return '☁️'
    if (w.includes('晴')) return '☀️'
    if (w.includes('雾')) return '🌫️'
    if (w.includes('雷')) return '⛈️'
    return '🌤️'
}
</script>

<style scoped>
.transit-module { display: flex; flex: 1; height: 100%; overflow: hidden; background: #f4f7fb; }
.map-shell { flex: 1; min-width: 0; position: relative; background: #e9eef5; }
.map-area { width: 100%; height: 100%; }
.map-error { position: absolute; left: 20px; right: 20px; bottom: 20px; padding: 12px 14px; background: #fff7ed; color: #9a3412; border: 1px solid #fed7aa; border-radius: 8px; box-shadow: 0 12px 28px rgba(15,23,42,.12); line-height: 1.5; }
.smart-panel { width: 480px; flex-shrink: 0; background: #f8fafc; border-left: 1px solid #dbe3ee; }
.panel-scroll-area { height: 100%; overflow-y: auto; padding: 20px; }

/* 头部 */
.smart-header { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.eyebrow { display: block; margin-bottom: 6px; color: #2563eb; font-size: 12px; font-weight: 700; }
.smart-header h2 { margin: 0; color: #0f172a; font-size: 20px; line-height: 1.35; }
.status-pill { height: 28px; white-space: nowrap; padding: 6px 10px; background: #eef2ff; color: #475569; border-radius: 999px; font-size: 12px; }
.status-pill.ready, .import-banner.success { background: #dcfce7; color: #166534; }
.import-banner { display: inline-flex; gap: 6px; align-items: center; padding: 7px 10px; margin-bottom: 12px; background: #eff6ff; color: #1d4ed8; border-radius: 999px; font-size: 12px; }

/* 可视化面板切换 */
.vis-tabs { display: flex; gap: 6px; margin-bottom: 14px; }
.vis-tabs button { flex: 1; height: 34px; border: 1px solid #dbe3ee; border-radius: 8px; background: #fff; color: #475569; cursor: pointer; font-size: 13px; }
.vis-tabs button.active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; font-weight: 700; }

/* 动画 */
.animate-in { animation: fadeSlideIn .45s ease-out; }
@keyframes fadeSlideIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

/* 指标卡片 */
.metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.metric-card, .metrics div { padding: 12px 10px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; text-align: center; }
.metric-card i { color: #2563eb; font-size: 16px; display: block; margin-bottom: 4px; }
.metric-card strong, .metrics strong { display: block; color: #2563eb; font-size: 22px; font-weight: 800; }
.metric-card span, .metrics span { color: #64748b; font-size: 12px; }

/* 仪表盘 */
.gauge-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; }
.gauge-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px; text-align: center; }
.gauge-label { font-size: 12px; font-weight: 700; color: #475569; margin-bottom: 2px; }
.gauge-dom { width: 100%; height: 160px; }

/* 天气详情卡片 */
.weather-detail-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%); border: 1px solid #bfdbfe; border-radius: 12px; margin-bottom: 14px; }
.weather-icon-large { font-size: 44px; }
.weather-detail-body strong { display: block; color: #0f172a; font-size: 16px; }
.weather-detail-stats { display: flex; gap: 12px; margin: 6px 0; color: #475569; font-size: 13px; }
.weather-detail-body em { color: #64748b; font-size: 12px; }

/* 段卡片列表 */
.segment-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; }
.segment-card { padding: 12px 14px; background: #fff; border: 1px solid #e2e8f0; border-left: 4px solid #2563eb; border-radius: 8px; }
.seg-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.seg-badge { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border-radius: 6px; color: #fff; font-size: 14px; }
.seg-header strong { color: #0f172a; font-size: 14px; }
.seg-stats { display: flex; gap: 12px; flex-wrap: wrap; }
.seg-stats span { color: #64748b; font-size: 12px; background: #f1f5f9; padding: 3px 8px; border-radius: 4px; }

/* 图表卡片 */
.viz-charts .chart-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
.chart-title { color: #0f172a; font-size: 13px; font-weight: 700; margin-bottom: 8px; }
.chart-dom { width: 100%; }
.chart-timeline { height: 220px; }

/* 通用卡片 */
.panel-card, .weather-card { margin-bottom: 14px; padding: 14px; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 8px 22px rgba(15,23,42,.04); }
.section-title { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #334155; font-size: 12px; font-weight: 700; }

.field-wrap { position: relative; }
.field-line { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; }
.point { width: 10px; height: 10px; flex-shrink: 0; border-radius: 999px; }
.point.start { background: #16a34a; } .point.end { background: #ef4444; }
input, select { width: 100%; min-width: 0; border: 0; outline: none; background: transparent; color: #0f172a; font-size: 14px; }
select { height: 42px; margin-top: 12px; padding: 0 12px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; }
.suggest-panel { position: absolute; z-index: 20; left: 0; right: 0; top: calc(100% + 6px); max-height: 280px; overflow-y: auto; padding: 6px; background: #fff; border: 1px solid #dbe3ee; border-radius: 8px; box-shadow: 0 16px 34px rgba(15,23,42,.16); }
.suggest-panel button, .route-list button { width: 100%; border: 0; text-align: left; cursor: pointer; }
.suggest-panel button { padding: 10px; border-radius: 6px; background: transparent; }
.suggest-panel button:hover { background: #eff6ff; }
.suggest-panel strong, .suggest-panel span, .route-list strong, .route-list span { display: block; }
.suggest-panel span, .empty-line, .weather-card p, .route-list span, .risk-card p { color: #64748b; font-size: 12px; line-height: 1.55; }

.swap-btn { width: 34px; height: 34px; margin: 8px auto; display: flex; align-items: center; justify-content: center; border: 1px solid #dbe3ee; border-radius: 999px; background: #fff; color: #2563eb; cursor: pointer; }
.mode-tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 12px 0; }
.mode-tabs button { height: 36px; border: 1px solid #dbe3ee; border-radius: 8px; background: #fff; color: #475569; cursor: pointer; }
.mode-tabs button.active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; font-weight: 700; }
.primary-btn, .secondary-btn { width: 100%; height: 44px; display: flex; align-items: center; justify-content: center; gap: 8px; border-radius: 8px; cursor: pointer; font-weight: 700; }
.primary-btn { border: 0; background: #2563eb; color: #fff; }
.primary-btn:disabled, .secondary-btn:disabled { opacity: .55; cursor: not-allowed; }
.secondary-btn { border: 1px solid #2563eb; background: #fff; color: #1d4ed8; }

.reason { color: #0f172a; font-size: 13px; line-height: 1.65; }
.weather-section { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.weather-card { margin-bottom: 0; min-height: 158px; }
.weather-head span { color: #64748b; font-size: 12px; font-weight: 700; }
.weather-head strong { display: block; margin-top: 4px; color: #0f172a; font-size: 15px; }
.temperature { margin-top: 12px; color: #2563eb; font-size: 30px; font-weight: 800; }
.weather-main { margin-bottom: 8px; color: #334155; font-weight: 700; }
.weather-card small { color: #94a3b8; font-size: 12px; }

.risk-level { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 8px; padding: 6px 10px; border-radius: 999px; font-weight: 800; }
.risk-level small { font-size: 11px; }
.risk-card.low .risk-level { background: #dcfce7; color: #166534; }
.risk-card.medium .risk-level { background: #fef3c7; color: #92400e; }
.risk-card.high .risk-level { background: #fee2e2; color: #991b1b; }
.route-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; }
.route-list button { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 13px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #0f172a; }
.route-list button.active { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,.12); }

@media (max-width: 900px) { .transit-module { flex-direction: column; } .map-shell { min-height: 45vh; } .smart-panel { width: 100%; height: 55vh; } }
</style>
