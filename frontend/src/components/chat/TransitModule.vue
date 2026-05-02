<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { queryRoutes } from '../../utils/api.js'

const props = defineProps({
    routeData: {
        type: Object,
        default: () => ({ from: '', to: '', preference: '', tripPlanId: null })
    }
})

const emit = defineEmits(['update:routeData', 'booking'])

const startPoint = ref(props.routeData.from || '')
const endPoint = ref(props.routeData.to || '')
const preference = ref(props.routeData.preference || '舒适优先')
const selectedMode = ref('SMART')
const loading = ref(false)
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

let map = null
let driving = null
let geocoder = null
let autoComplete = null
let startMarker = null
let endMarker = null
let startSuggestTimer = null
let endSuggestTimer = null

const canSearch = computed(() => Boolean(startPoint.value.trim() && endPoint.value.trim()))

const routeTitle = computed(() => {
    if (!startPoint.value && !endPoint.value) return '选择具体地点生成路线'
    return `${startPoint.value || '出发地'} → ${endPoint.value || '目的地'}`
})

const weatherAlerts = computed(() => [
    buildWeatherAlert('出发地', startWeather.value),
    buildWeatherAlert('目的地', endWeather.value)
].filter(Boolean))

const guardItems = computed(() => {
    const defaults = ['实时位置守护', '轨迹回放', '电子围栏提醒', '异常停留与偏航提醒']
    return (serviceCapabilities.value.length ? serviceCapabilities.value : defaults).map((name, index) => ({
        name,
        status: index === 0 && activeRoute.value ? '已就绪' : '可开启'
    }))
})

watch(() => props.routeData, value => {
    startPoint.value = value?.from || ''
    endPoint.value = value?.to || ''
    preference.value = value?.preference || preference.value || '舒适优先'
    selectedStartPlace.value = null
    selectedEndPlace.value = null
}, { deep: true })

watch([startPoint, endPoint, preference], () => {
    emit('update:routeData', {
        ...props.routeData,
        from: startPoint.value,
        to: endPoint.value,
        preference: preference.value
    })
})

async function initMap() {
    if (map) return
    await nextTick()

    if (!window.AMap) {
        mapError.value = '地图服务暂时不可用，请检查高德地图 Key 或网络连接。'
        return
    }

    window.AMap.plugin(['AMap.Geocoder', 'AMap.Driving', 'AMap.Scale', 'AMap.ToolBar', 'AMap.AutoComplete'], () => {
        map = new window.AMap.Map('container', {
            zoom: 11,
            center: [118.796877, 32.060255],
            resizeEnable: true,
            viewMode: '2D'
        })
        map.addControl(new window.AMap.Scale())
        map.addControl(new window.AMap.ToolBar({ position: 'RB' }))
        geocoder = new window.AMap.Geocoder({ city: '全国' })
        autoComplete = new window.AMap.AutoComplete({ city: '全国', citylimit: false })
        driving = new window.AMap.Driving({
            map,
            policy: window.AMap.DrivingPolicy.LEAST_TIME,
            autoFitView: true
        })
        mapReady.value = true
    })
}

function destroyMap() {
    if (startSuggestTimer) clearTimeout(startSuggestTimer)
    if (endSuggestTimer) clearTimeout(endSuggestTimer)
    if (map) {
        map.destroy()
        map = null
        driving = null
        geocoder = null
        autoComplete = null
        startMarker = null
        endMarker = null
        mapReady.value = false
    }
}

function onPlaceInput(type) {
    const keyword = type === 'start' ? startPoint.value.trim() : endPoint.value.trim()
    if (type === 'start') {
        selectedStartPlace.value = null
        startSuggestions.value = []
        if (startSuggestTimer) clearTimeout(startSuggestTimer)
        startSuggestTimer = setTimeout(() => searchPlaces(keyword, 'start'), 260)
    } else {
        selectedEndPlace.value = null
        endSuggestions.value = []
        if (endSuggestTimer) clearTimeout(endSuggestTimer)
        endSuggestTimer = setTimeout(() => searchPlaces(keyword, 'end'), 260)
    }
}

async function searchPlaces(keyword, type) {
    if (!keyword || keyword.length < 2) return
    if (!map) await initMap()
    if (!autoComplete) return

    autoComplete.search(keyword, (status, result) => {
        const tips = status === 'complete' && Array.isArray(result?.tips) ? result.tips : []
        const places = tips
            .filter(item => item.name && item.location)
            .slice(0, 8)
            .map(item => ({
                name: item.name,
                district: item.district || '',
                address: item.address || '',
                adcode: item.adcode || '',
                location: `${item.location.lng},${item.location.lat}`
            }))
        if (type === 'start') {
            startSuggestions.value = places
        } else {
            endSuggestions.value = places
        }
    })
}

async function selectPlace(type, place) {
    if (type === 'start') {
        selectedStartPlace.value = place
        startPoint.value = place.name
        startSuggestions.value = []
        startLocation.value = toLocationInfo(place)
    } else {
        selectedEndPlace.value = place
        endPoint.value = place.name
        endSuggestions.value = []
        endLocation.value = toLocationInfo(place)
    }
    if (canSearch.value) {
        await drawAmapRoute()
    } else {
        previewSelectedPoints()
    }
}

function placeLocation(place) {
    if (!place?.location) return ''
    if (typeof place.location === 'string') return place.location
    if (place.location.lng !== undefined && place.location.lat !== undefined) {
        return `${place.location.lng},${place.location.lat}`
    }
    return ''
}

function toLocationInfo(place) {
    return {
        name: place.name,
        formattedAddress: [place.district, place.address, place.name].filter(Boolean).join(''),
        location: placeLocation(place),
        adcode: place.adcode,
        city: place.district
    }
}

function safeLocationInfo(place, keyword) {
    if (place) return toLocationInfo(place)
    return {
        name: keyword,
        formattedAddress: keyword,
        location: '',
        adcode: '',
        city: ''
    }
}

async function searchSmartRoute(showTip = true) {
    if (!canSearch.value) {
        if (showTip) {
            mapError.value = '请先从高德候选地点中选择出发地和目的地，再生成路线。'
        }
        return
    }
    if (!map) await initMap()

    loading.value = true
    mapError.value = ''

    try {
        const userId = sessionStorage.getItem('loginUser') || 'guest'
        const plan = await queryRoutes({
            userId,
            tripPlanId: props.routeData.tripPlanId,
            startPoint: startPoint.value.trim(),
            endPoint: endPoint.value.trim(),
            startLocation: placeLocation(selectedStartPlace.value) || startLocation.value?.location || '',
            endLocation: placeLocation(selectedEndPlace.value) || endLocation.value?.location || '',
            startAdcode: selectedStartPlace.value?.adcode || startLocation.value?.adcode || '',
            endAdcode: selectedEndPlace.value?.adcode || endLocation.value?.adcode || '',
            startCity: selectedStartPlace.value?.district || startLocation.value?.city || '',
            endCity: selectedEndPlace.value?.district || endLocation.value?.city || '',
            routeType: selectedMode.value,
            preference: preference.value
        })

        startLocation.value = plan.startLocation || safeLocationInfo(selectedStartPlace.value, startPoint.value)
        endLocation.value = plan.endLocation || safeLocationInfo(selectedEndPlace.value, endPoint.value)
        startWeather.value = plan.startWeather || null
        endWeather.value = plan.endWeather || null
        serviceCapabilities.value = plan.serviceCapabilities || []
        routeResults.value = plan.routes || []
        activeRoute.value = routeResults.value.find(item => item.routeType === selectedMode.value) || routeResults.value[0] || null
        await drawAmapRoute()
    } catch (error) {
        mapError.value = friendlyError(error)
        await previewSelectedPoints()
    } finally {
        loading.value = false
    }
}

async function drawAmapRoute() {
    return new Promise(async resolve => {
        if (!window.AMap || !map || !driving || !canSearch.value) {
            resolve()
            return
        }
        driving.clear()
        clearMarkers()
        const startLngLat = await resolveLngLat('start')
        const endLngLat = await resolveLngLat('end')
        if (!startLngLat || !endLngLat) {
            mapError.value = '起点或终点还没有解析到坐标，请从候选地点中选择，或输入更完整的地址。'
            resolve()
            return
        }
        drawByLngLat(startLngLat, endLngLat, resolve)
    })
}

function resolveLngLat(type) {
    const selected = type === 'start' ? selectedStartPlace.value : selectedEndPlace.value
    const cached = type === 'start' ? startLocation.value : endLocation.value
    const keyword = type === 'start' ? startPoint.value.trim() : endPoint.value.trim()
    const direct = parseLocation(cached?.location || placeLocation(selected))
    if (direct) return Promise.resolve(direct)
    return geocodePoint(keyword, type)
}

function geocodePoint(keyword, type) {
    return new Promise(resolve => {
        if (!keyword || !geocoder) {
            resolve(null)
            return
        }
        geocoder.getLocation(keyword, (status, result) => {
            const first = status === 'complete' ? result?.geocodes?.[0] : null
            const lngLat = first?.location ? [first.location.lng, first.location.lat] : null
            if (lngLat) {
                const info = {
                    name: keyword,
                    formattedAddress: first.formattedAddress || keyword,
                    location: `${first.location.lng},${first.location.lat}`,
                    adcode: first.adcode || '',
                    city: first.city || first.district || ''
                }
                if (type === 'start') {
                    startLocation.value = info
                    selectedStartPlace.value = selectedStartPlace.value || info
                } else {
                    endLocation.value = info
                    selectedEndPlace.value = selectedEndPlace.value || info
                }
            }
            resolve(lngLat)
        })
    })
}

function previewSelectedPoints() {
    if (!map) return
    clearMarkers()
    const startLngLat = parseLocation(placeLocation(selectedStartPlace.value) || startLocation.value?.location)
    const endLngLat = parseLocation(placeLocation(selectedEndPlace.value) || endLocation.value?.location)
    const markers = []
    if (startLngLat) {
        startMarker = new window.AMap.Marker({ position: startLngLat, map, label: { content: '起点', direction: 'top' } })
        markers.push(startMarker)
    }
    if (endLngLat) {
        endMarker = new window.AMap.Marker({ position: endLngLat, map, label: { content: '终点', direction: 'top' } })
        markers.push(endMarker)
    }
    if (markers.length) map.setFitView(markers)
}

function drawByLngLat(startLngLat, endLngLat, resolve) {
    startMarker = new window.AMap.Marker({ position: startLngLat, map, label: { content: '起点', direction: 'top' } })
    endMarker = new window.AMap.Marker({ position: endLngLat, map, label: { content: '终点', direction: 'top' } })
    driving.search(startLngLat, endLngLat, status => {
        if (status !== 'complete') {
            map.setFitView([startMarker, endMarker])
            if (!mapError.value) mapError.value = '地图路线绘制失败，但右侧方案已返回。'
        }
        resolve()
    })
}

function parseLocation(location) {
    if (!location || !String(location).includes(',')) return null
    const [lng, lat] = String(location).split(',').map(Number)
    if (Number.isNaN(lng) || Number.isNaN(lat)) return null
    return [lng, lat]
}

function clearMarkers() {
    if (startMarker) {
        map.remove(startMarker)
        startMarker = null
    }
    if (endMarker) {
        map.remove(endMarker)
        endMarker = null
    }
}

function swapPlaces() {
    const oldStartText = startPoint.value
    const oldStartPlace = selectedStartPlace.value
    startPoint.value = endPoint.value
    selectedStartPlace.value = selectedEndPlace.value
    endPoint.value = oldStartText
    selectedEndPlace.value = oldStartPlace
    startSuggestions.value = []
    endSuggestions.value = []
    if (canSearch.value) {
        drawAmapRoute()
    } else {
        previewSelectedPoints()
    }
}

async function applyRoute(route) {
    activeRoute.value = route
    selectedMode.value = route.routeType || 'SMART'
    if (driving && typeof driving.setPolicy === 'function') {
        const policy = route.routeType === 'ECO'
            ? window.AMap.DrivingPolicy.LEAST_FEE
            : window.AMap.DrivingPolicy.LEAST_TIME
        driving.setPolicy(policy)
    }
    await drawAmapRoute()
}

function weatherCity(weather, fallbackLocation) {
    return weather?.city || fallbackLocation?.city || fallbackLocation?.district || fallbackLocation?.name || '未知城市'
}

function buildWeatherAlert(label, weather) {
    if (!weather) return null
    const text = `${weather.weather || ''}${weather.windpower || ''}`
    const temperature = Number(weather.temperature)
    if (text.includes('雨') || text.includes('雪') || text.includes('雾')) return `${label}有${weather.weather}，建议预留进站和换乘时间。`
    if (!Number.isNaN(temperature) && temperature >= 32) return `${label}气温较高，建议安排室内候车和补水。`
    if (!Number.isNaN(temperature) && temperature <= 3) return `${label}气温较低，建议关注道路结冰和保暖。`
    if (text.includes('5') || text.includes('6') || text.includes('7') || text.includes('8')) return `${label}风力偏大，建议关注航班和高架道路通行变化。`
    return `${label}天气适合出行，按当前方案执行即可。`
}

function friendlyError(error) {
    const message = error?.message || ''
    if (message.includes('AI') || message.includes('智谱')) return message
    if (message.includes('ENGINE_RESPONSE_DATA_ERROR') || message.includes('geocode')) {
        return '地点解析失败。请从高德候选地点中选择具体地点，并确认后端使用的是高德 Web 服务 Key。'
    }
    return message || '路线规划失败，请稍后再试。'
}

defineExpose({ initMap, destroyMap, searchSmartRoute })

onMounted(initMap)
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
                <header class="smart-header">
                    <div>
                        <span class="eyebrow">智能路线</span>
                        <h2>{{ routeTitle }}</h2>
                    </div>
                    <span class="status-pill" :class="{ ready: mapReady }">{{ mapReady ? '地图已连接' : '地图加载中' }}</span>
                </header>

                <section class="search-box">
                    <div class="field-wrap">
                        <div class="field-line">
                            <span class="point start"></span>
                            <input v-model="startPoint" placeholder="搜索并选择出发地" @input="onPlaceInput('start')" />
                        </div>
                        <div v-if="startSuggestions.length" class="suggest-panel">
                            <button v-for="item in startSuggestions" :key="item.name + item.location" @click="selectPlace('start', item)">
                                <strong>{{ item.name }}</strong>
                                <span>{{ item.district }}{{ item.address }}</span>
                            </button>
                        </div>
                        <div v-else-if="startPoint && !selectedStartPlace" class="select-hint">请从候选地点中选择具体位置</div>
                    </div>

                    <button class="swap-btn" @click="swapPlaces" title="交换起终点">
                        <i class="fa-solid fa-right-left"></i>
                    </button>

                    <div class="field-wrap">
                        <div class="field-line">
                            <span class="point end"></span>
                            <input v-model="endPoint" placeholder="搜索并选择目的地" @input="onPlaceInput('end')" />
                        </div>
                        <div v-if="endSuggestions.length" class="suggest-panel">
                            <button v-for="item in endSuggestions" :key="item.name + item.location" @click="selectPlace('end', item)">
                                <strong>{{ item.name }}</strong>
                                <span>{{ item.district }}{{ item.address }}</span>
                            </button>
                        </div>
                        <div v-else-if="endPoint && !selectedEndPlace" class="select-hint">请从候选地点中选择具体位置</div>
                    </div>

                    <div class="preference-row">
                        <select v-model="preference">
                            <option>舒适优先</option>
                            <option>少换乘</option>
                            <option>时间优先</option>
                            <option>经济优先</option>
                            <option>适合亲子/老人</option>
                        </select>
                    </div>

                    <div class="mode-tabs">
                        <button :class="{ active: selectedMode === 'SMART' }" @click="selectedMode = 'SMART'">推荐</button>
                        <button :class="{ active: selectedMode === 'FAST' }" @click="selectedMode = 'FAST'">快速</button>
                        <button :class="{ active: selectedMode === 'ECO' }" @click="selectedMode = 'ECO'">经济</button>
                    </div>

                    <button class="primary-btn" :disabled="loading || !canSearch" @click="searchSmartRoute(true)">
                        <i class="fa-solid fa-route"></i>
                        <span>{{ loading ? 'AI 正在生成...' : '生成路线方案' }}</span>
                    </button>
                </section>

                <section v-if="startLocation || endLocation" class="location-card">
                    <div>
                        <span>出发地</span>
                        <strong>{{ startLocation?.formattedAddress || startPoint }}</strong>
                    </div>
                    <i class="fa-solid fa-arrow-right"></i>
                    <div>
                        <span>目的地</span>
                        <strong>{{ endLocation?.formattedAddress || endPoint }}</strong>
                    </div>
                </section>

                <section class="weather-section">
                    <article class="weather-card">
                        <div class="weather-head">
                            <span>出发地天气</span>
                            <strong>{{ weatherCity(startWeather, startLocation) }}</strong>
                        </div>
                        <template v-if="startWeather">
                            <div class="temperature">{{ startWeather.temperature || '-' }}℃</div>
                            <div class="weather-main">{{ startWeather.weather || '暂无天气' }}</div>
                            <dl>
                                <div><dt>湿度</dt><dd>{{ startWeather.humidity || '-' }}%</dd></div>
                                <div><dt>风向</dt><dd>{{ startWeather.winddirection || '-' }}</dd></div>
                                <div><dt>风力</dt><dd>{{ startWeather.windpower || '-' }}</dd></div>
                            </dl>
                            <small>{{ startWeather.reporttime || '暂无发布时间' }}</small>
                        </template>
                        <p v-else class="empty-weather">暂无天气数据</p>
                    </article>

                    <article class="weather-card">
                        <div class="weather-head">
                            <span>目的地天气</span>
                            <strong>{{ weatherCity(endWeather, endLocation) }}</strong>
                        </div>
                        <template v-if="endWeather">
                            <div class="temperature">{{ endWeather.temperature || '-' }}℃</div>
                            <div class="weather-main">{{ endWeather.weather || '暂无天气' }}</div>
                            <dl>
                                <div><dt>湿度</dt><dd>{{ endWeather.humidity || '-' }}%</dd></div>
                                <div><dt>风向</dt><dd>{{ endWeather.winddirection || '-' }}</dd></div>
                                <div><dt>风力</dt><dd>{{ endWeather.windpower || '-' }}</dd></div>
                            </dl>
                            <small>{{ endWeather.reporttime || '暂无发布时间' }}</small>
                        </template>
                        <p v-else class="empty-weather">暂无天气数据</p>
                    </article>
                </section>

                <section v-if="weatherAlerts.length" class="advice-card">
                    <div class="section-title">
                        <i class="fa-solid fa-cloud-sun-rain"></i>
                        <span>天气出行建议</span>
                    </div>
                    <p v-for="item in weatherAlerts" :key="item">{{ item }}</p>
                </section>

                <section v-if="activeRoute" class="route-summary">
                    <div class="section-title">
                        <i class="fa-solid fa-wand-magic-sparkles"></i>
                        <span>AI 推荐方案</span>
                    </div>
                    <h3>{{ activeRoute.routeName }}</h3>
                    <div class="metrics">
                        <div><strong>{{ activeRoute.duration }}</strong><span>分钟</span></div>
                        <div><strong>{{ activeRoute.distance }}</strong><span>公里</span></div>
                        <div><strong>{{ activeRoute.cost }}</strong><span>元预估</span></div>
                    </div>
                    <p>{{ activeRoute.description }}</p>
                    <p class="reason">{{ activeRoute.recommendReason }}</p>
                </section>

                <section v-if="routeResults.length" class="route-list">
                    <button
                        v-for="route in routeResults"
                        :key="route.id || route.routeType"
                        :class="{ active: activeRoute && activeRoute.routeType === route.routeType }"
                        @click="applyRoute(route)"
                    >
                        <div>
                            <strong>{{ route.routeName }}</strong>
                            <span>{{ route.duration }} 分钟 · {{ route.distance }} 公里 · 约 {{ route.cost }} 元</span>
                        </div>
                        <i class="fa-solid fa-chevron-right"></i>
                    </button>
                </section>

                <section class="guard-card">
                    <div class="section-title">
                        <i class="fa-solid fa-shield-heart"></i>
                        <span>行程守护</span>
                    </div>
                    <div class="guard-grid">
                        <div v-for="item in guardItems" :key="item.name" class="guard-item">
                            <strong>{{ item.name }}</strong>
                            <span>{{ item.status }}</span>
                        </div>
                    </div>
                </section>

                <button class="secondary-btn" @click="emit('booking')">
                    <i class="fa-solid fa-paper-plane"></i>
                    <span>继续生成出行服务</span>
                </button>
            </div>
        </aside>
    </div>
</template>

<style scoped>
.transit-module {
    display: flex;
    flex: 1;
    height: 100%;
    overflow: hidden;
    background: #f4f7fb;
}

.map-shell {
    flex: 1;
    min-width: 0;
    position: relative;
    background: #e9eef5;
}

.map-area {
    width: 100%;
    height: 100%;
}

.map-error {
    position: absolute;
    left: 20px;
    right: 20px;
    bottom: 20px;
    padding: 12px 14px;
    background: #fff7ed;
    color: #9a3412;
    border: 1px solid #fed7aa;
    border-radius: 8px;
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
    line-height: 1.5;
}

.smart-panel {
    width: 448px;
    flex-shrink: 0;
    background: #f8fafc;
    border-left: 1px solid #dbe3ee;
}

.panel-scroll-area {
    height: 100%;
    overflow-y: auto;
    padding: 20px;
}

.smart-header {
    display: flex;
    justify-content: space-between;
    gap: 14px;
    margin-bottom: 16px;
}

.eyebrow {
    display: block;
    margin-bottom: 6px;
    color: #2563eb;
    font-size: 12px;
    font-weight: 700;
}

.smart-header h2 {
    margin: 0;
    color: #0f172a;
    font-size: 20px;
    line-height: 1.35;
}

.status-pill {
    height: 28px;
    white-space: nowrap;
    padding: 6px 10px;
    border-radius: 999px;
    background: #eef2ff;
    color: #475569;
    font-size: 12px;
}

.status-pill.ready {
    background: #dcfce7;
    color: #166534;
}

.search-box,
.location-card,
.weather-card,
.advice-card,
.route-summary,
.guard-card {
    margin-bottom: 14px;
    padding: 14px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.field-wrap {
    position: relative;
}

.field-line {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
}

.point {
    width: 10px;
    height: 10px;
    flex-shrink: 0;
    border-radius: 999px;
}

.point.start {
    background: #16a34a;
}

.point.end {
    background: #ef4444;
}

input,
select {
    width: 100%;
    min-width: 0;
    border: 0;
    outline: none;
    background: transparent;
    color: #0f172a;
    font-size: 14px;
}

select {
    height: 42px;
    padding: 0 12px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #ffffff;
}

.suggest-panel {
    position: absolute;
    z-index: 20;
    left: 0;
    right: 0;
    top: calc(100% + 6px);
    max-height: 280px;
    overflow-y: auto;
    padding: 6px;
    background: #ffffff;
    border: 1px solid #dbe3ee;
    border-radius: 8px;
    box-shadow: 0 16px 34px rgba(15, 23, 42, 0.16);
}

.suggest-panel button {
    width: 100%;
    padding: 10px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    text-align: left;
    cursor: pointer;
}

.suggest-panel button:hover {
    background: #eff6ff;
}

.suggest-panel strong,
.suggest-panel span {
    display: block;
}

.suggest-panel strong {
    color: #0f172a;
    font-size: 14px;
}

.suggest-panel span,
.select-hint {
    margin-top: 4px;
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
}

.select-hint {
    padding-left: 4px;
}

.swap-btn {
    width: 34px;
    height: 34px;
    margin: 8px auto;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #dbe3ee;
    border-radius: 999px;
    background: #ffffff;
    color: #2563eb;
    cursor: pointer;
}

.preference-row {
    margin-top: 12px;
}

.mode-tabs {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin: 12px 0;
}

.mode-tabs button {
    height: 36px;
    border: 1px solid #dbe3ee;
    border-radius: 8px;
    background: #ffffff;
    color: #475569;
    cursor: pointer;
}

.mode-tabs button.active {
    border-color: #2563eb;
    background: #eff6ff;
    color: #1d4ed8;
    font-weight: 700;
}

.primary-btn,
.secondary-btn {
    width: 100%;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 700;
}

.primary-btn {
    border: 0;
    background: #2563eb;
    color: #ffffff;
}

.primary-btn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.secondary-btn {
    border: 1px solid #2563eb;
    background: #ffffff;
    color: #1d4ed8;
}

.location-card {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
    align-items: center;
    gap: 10px;
}

.location-card span {
    display: block;
    margin-bottom: 4px;
    color: #64748b;
    font-size: 12px;
}

.location-card strong {
    display: block;
    color: #0f172a;
    font-size: 13px;
    line-height: 1.5;
}

.location-card i {
    color: #94a3b8;
}

.weather-section {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 14px;
}

.weather-card {
    margin-bottom: 0;
    min-height: 188px;
}

.weather-head span,
.section-title {
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
}

.weather-head strong {
    display: block;
    margin-top: 4px;
    color: #0f172a;
    font-size: 15px;
}

.temperature {
    margin-top: 12px;
    color: #2563eb;
    font-size: 30px;
    font-weight: 800;
}

.weather-main {
    margin-bottom: 10px;
    color: #334155;
    font-weight: 700;
}

dl {
    margin: 0;
}

dl div {
    display: flex;
    justify-content: space-between;
    margin-top: 5px;
    color: #64748b;
    font-size: 12px;
}

dt,
dd {
    margin: 0;
}

.weather-card small,
.empty-weather {
    display: block;
    margin-top: 10px;
    color: #94a3b8;
    font-size: 12px;
    line-height: 1.5;
}

.section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    color: #334155;
}

.advice-card p,
.route-summary p {
    margin: 8px 0 0;
    color: #475569;
    font-size: 13px;
    line-height: 1.65;
}

.route-summary h3 {
    margin: 4px 0 12px;
    color: #0f172a;
    font-size: 18px;
}

.metrics {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
}

.metrics div {
    padding: 10px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
}

.metrics strong {
    display: block;
    color: #2563eb;
    font-size: 22px;
}

.metrics span {
    color: #64748b;
    font-size: 12px;
}

.reason {
    color: #0f172a !important;
}

.route-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 14px;
}

.route-list button {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 13px 14px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #ffffff;
    color: #0f172a;
    text-align: left;
    cursor: pointer;
}

.route-list button.active {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.route-list strong,
.route-list span {
    display: block;
}

.route-list span {
    margin-top: 4px;
    color: #64748b;
    font-size: 12px;
}

.guard-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.guard-item {
    padding: 10px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
}

.guard-item strong {
    display: block;
    color: #0f172a;
    font-size: 13px;
    line-height: 1.4;
}

.guard-item span {
    display: inline-block;
    margin-top: 8px;
    padding: 3px 8px;
    border-radius: 999px;
    background: #eef2ff;
    color: #1d4ed8;
    font-size: 12px;
}

@media (max-width: 900px) {
    .transit-module {
        flex-direction: column;
    }

    .map-shell {
        min-height: 45vh;
    }

    .smart-panel {
        width: 100%;
        height: 55vh;
    }
}
</style>
