<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import LandingSidebar from './components/LandingSidebar.vue'
import HomeHeroHeader from './components/HomeHeroHeader.vue'
import HeroIntro from './components/HeroIntro.vue'
import TravelAssistantPanel from './components/TravelAssistantPanel.vue'
import {
  logoUrl,
  heroBgUrl,
  hotelResultBgUrl,
  driveResultBgUrl,
  busResultBgUrl,
  subwayResultBgUrl,
  trainResultBgUrl,
  flightResultBgUrl,
  cruiseResultBgUrl,
  xiaoqiAvatar,
  hotDemandChips,
  planningStageMeta,
  planningStageOrder,
  enumLabelMap,
  travelTabs,
  sidebarEntries,
  orderCategories
} from './landingConfig.js'
import {
  askAssistant,
  cancelBookingOrder,
  createAfterSaleOrder,
  createBookingOrder,
  createHotelOrder,
  createDebouncedLocationTips,
  getHotelDetail,
  getLocationTips,
  getWeather,
  listUserOrders,
  payBookingOrder,
  planTravelChain,
  searchHotels,
  searchTraffic
} from '../../api/landingApi.js'

// ★ 每个地点输入框独立持有防抖实例，避免交叉取消
const hotelDebouncedTips = createDebouncedLocationTips()
const serviceDebouncedTips = createDebouncedLocationTips()
const chainDebouncedTips = createDebouncedLocationTips()

const props = defineProps({
  isLoggedIn: { type: Boolean, default: false },
  loginUser: { type: String, default: '' }
})

const emit = defineEmits(['start', 'require-login'])

const activeTabIndex = ref(0)
const activeDriveTab = ref('rentalCar')
const isSceneCollapsed = ref(false)
const showOrderDropdown = ref(false)
const orderFilter = ref('all')
const showAssistant = ref(false)
const assistantScene = ref('旅游场景')
const assistantInput = ref('')
const assistantLoading = ref(false)
const assistantBodyRef = ref(null)
const assistantMessages = ref([
  { role: 'assistant', content: '你好，我是小七。我可以帮你一键预约酒店机票，并提供智能售后服务。' }
])
const assistantOrder = ref(null)
const resultFromSidebar = ref(false)

const assistantScenes = ['通勤', '商务出行', '旅游场景', '特殊群体']

const searchForm = ref({
  startPlace: '',
  destination: '',
  date: '',
  people: '1位旅客',
  extra: '舒适优先'
})

const hotelSearchForm = ref({
  destinationKeyword: '',
  selectedLocation: null,
  checkInDate: todayString(),
  checkOutDate: offsetDateString(1),
  adults: 2,
  children: 0,
  rooms: 1,
  preference: '舒适优先',
  minPrice: '',
  maxPrice: ''
})
const hotelLocationTips = ref([])
const hotelTipLoading = ref(false)
const showHotelTips = ref(false)
const showGuestPicker = ref(false)
const serviceStartLocation = ref(null)
const serviceEndLocation = ref(null)
const serviceStartTips = ref([])
const serviceEndTips = ref([])
const serviceStartTipLoading = ref(false)
const serviceEndTipLoading = ref(false)
const showServiceStartTips = ref(false)
const showServiceEndTips = ref(false)

const privateCarForm = ref({
  startPlace: '杭州东站',
  endPlace: '西湖湖滨',
  date: '2026-05-25',
  startTime: '10:20',
  preference: '舒适型'
})

const rentalCarForm = ref({
  pickupPlace: '杭州东站',
  returnPlace: '与取车相同',
  date: '2026-05-25',
  pickupTime: '10:30',
  returnTime: '18:00'
})

const loading = ref(false)
const resultType = ref('')
const currentResultTabId = ref('hotel')
const hotelResults = ref([])
const hotelDetail = ref(null)
const pageMode = ref('home')
const trafficResults = ref([])
const recommendedHotels = ref([])
const recommendedTraffic = ref([])
const orders = ref([])
const orderModal = ref(null)
const lastMessage = ref('')
const chainStartPlace = ref('')
const chainEndPlace = ref('')
const chainInput = ref('')
const chainStartLocation = ref(null)
const chainEndLocation = ref(null)
const chainStartTips = ref([])
const chainEndTips = ref([])
const chainStartTipLoading = ref(false)
const chainEndTipLoading = ref(false)
const showChainStartTips = ref(false)
const showChainEndTips = ref(false)
const chainLoading = ref(false)
const chainResult = ref(null)
const afterSaleLoading = ref(false)
const locatingStartPlace = ref(false)
const chainAnimating = ref(false)
const visiblePlanningStages = ref([])
const planningStageStates = ref(createInitialPlanningStageStates())
const chainSupplementInput = ref('')
const chainOrderPreview = ref(null)
const weatherLoading = ref(false)
const landingMapError = ref('')
let landingMap = null
let landingMapOverlays = []
const activeTab = computed(() => travelTabs[activeTabIndex.value] || travelTabs[0])
const activeMainTab = computed(() => activeTab.value.id)
const isDrive = computed(() => activeMainTab.value === 'selfDrive')
const resultTab = computed(() => travelTabs.find((tab) => tab.id === currentResultTabId.value) || travelTabs[0])
const resultMainTab = computed(() => resultTab.value.id)
const resultIsDrive = computed(() => resultMainTab.value === 'selfDrive')
const isAuthenticated = computed(() => Boolean(props.isLoggedIn || sessionStorage.getItem('loginUser') || sessionStorage.getItem('loginUserId')))
const currentUserId = computed(() => props.loginUser || sessionStorage.getItem('loginUserId') || sessionStorage.getItem('loginUser') || '')
const currentUserName = computed(() => props.loginUser || sessionStorage.getItem('loginUser') || '用户')
const orderBadgeCount = computed(() => orders.value.length)
const filteredOrders = computed(() => orders.value.filter((order) => orderMatchesCategory(order, orderFilter.value)))
const orderPanelTitle = computed(() => orderCategories.find((item) => item.id === orderFilter.value)?.name || '我的订单')
const destinationCities = computed(() => {
  const cities = recommendedHotels.value.map((hotel) => hotel.city).filter(Boolean)
  return Array.from(new Set(cities)).slice(0, 5)
})
const featuredHotel = computed(() => recommendedHotels.value[0] || null)
const hotelGuestText = computed(() => `${hotelSearchForm.value.adults} 位成人，${hotelSearchForm.value.children} 位儿童，${hotelSearchForm.value.rooms} 间房`)
const resultHeroBg = computed(() => {
  const bgMap = {
    hotel: hotelResultBgUrl,
    selfDrive: driveResultBgUrl,
    bus: busResultBgUrl,
    subway: subwayResultBgUrl,
    train: trainResultBgUrl,
    flight: flightResultBgUrl,
    cruise: cruiseResultBgUrl
  }
  return bgMap[currentResultTabId.value] || hotelResultBgUrl
})
const normalizedChainResult = computed(() => normalizeChainResult(chainResult.value))
const availablePlanningStages = computed(() => buildAvailablePlanningStages(normalizedChainResult.value))
const hasCurrentItinerary = computed(() => Boolean(chainResult.value) && !needsClarification(normalizedChainResult.value))
const itinerarySummaryCards = computed(() => buildItinerarySummaryCards(normalizedChainResult.value))
const landingMapPoints = computed(() => buildLandingMapPoints(normalizedChainResult.value))
const landingMapSegments = computed(() => buildLandingMapSegments(normalizedChainResult.value))
const landingMapWeatherCards = computed(() => buildLandingMapWeatherCards(normalizedChainResult.value))
const landingMapReminderCards = computed(() => buildLandingMapReminderCards(normalizedChainResult.value))
const itineraryIntentFields = computed(() => buildItineraryIntentFields(normalizedChainResult.value))
const itineraryRouteCards = computed(() => buildItineraryRouteCards(normalizedChainResult.value))
const itineraryTrafficCards = computed(() => buildItineraryTrafficCards(normalizedChainResult.value))
const itineraryHotelCards = computed(() => buildItineraryHotelCards(normalizedChainResult.value))
const itineraryRiskCards = computed(() => buildItineraryRiskCards(normalizedChainResult.value))
const itineraryTimelineGroups = computed(() => buildItineraryTimelineGroups(normalizedChainResult.value))
const itineraryOrderItems = computed(() => buildItineraryOrderItems(normalizedChainResult.value))

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

function offsetDateString(offset) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return date.toISOString().slice(0, 10)
}

function buildTabConfig(tabId) {
  const map = {
    subway: ['去哪里？', '地铁站、商圈或目的地', '乘客', '1位乘客', '偏好', '少换乘、少步行'],
    bus: ['去哪里？', '公交站、客运站或目的地', '乘客', '1位乘客', '偏好', '少换乘、低预算'],
    train: ['去哪里？', '例如：杭州东站', '旅客', '2位乘客', '席别', '二等座、商务座'],
    flight: ['去哪里？', '例如：杭州萧山机场', '旅客', '1位成人', '偏好', '直飞、低价、上午出发'],
    cruise: ['去哪里？', '港口、航线或目的地', '旅客', '2位旅客', '偏好', '亲子、海景房']
  }
  const item = map[tabId] || map.train
  return {
    destinationLabel: item[0],
    destinationPlaceholder: item[1],
    peopleLabel: item[2],
    peoplePlaceholder: item[3],
    extraLabel: item[4],
    extraPlaceholder: item[5]
  }
}

const tabConfig = computed(() => buildTabConfig(activeMainTab.value))
const resultTabConfig = computed(() => buildTabConfig(resultMainTab.value))

function enterApp() {
  emit('start')
}

function openLoginModal() {
  emit('require-login', '登录后可同步订单、AI 助手会话和个性化出行偏好。')
}

function requireLoggedIn(message = '请先登录后再继续操作。') {
  if (isAuthenticated.value && currentUserId.value) return true
  emit('require-login', message)
  return false
}

function handleMainTabClick(index) {
  activeTabIndex.value = index
  if (travelTabs[index]?.id === 'selfDrive' && !activeDriveTab.value) {
    activeDriveTab.value = 'rentalCar'
  }
}

function scrollToHero() {
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    const target = document.querySelector('#hero')
    if (shell && target) shell.scrollTo({ top: target.offsetTop, behavior: 'smooth' })
  })
}

function scrollToBooking() {
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    const target = document.querySelector('.booking-section')
    if (shell && target) shell.scrollTo({ top: target.offsetTop - 18, behavior: 'smooth' })
  })
}

async function handleSidebarEntry(item) {
  if (item.divider) return
  if (item.action === 'map') {
    openLandingMapPage()
    return
  }
  if (item.action === 'orders') {
    if (!requireLoggedIn('登录后才能查看自己的历史订单。')) return
    await openOrderCategory('all')
    return
  }
  if (item.tabId) {
    openServiceSearchPage(item.tabId)
    return
  }
}

function isSidebarEntryActive(item) {
  if (item.action === 'map') return pageMode.value === 'map'
  if (item.action === 'orders') return pageMode.value === 'orders'
  if (item.tabId) return pageMode.value !== 'home' && currentResultTabId.value === item.tabId
  return false
}

function trafficType(tabId = activeMainTab.value) {
  if (tabId === 'train') return 'TRAIN'
  if (tabId === 'flight') return 'FLIGHT'
  if (tabId === 'bus') return 'BUS'
  if (tabId === 'subway') return 'METRO'
  if (tabId === 'selfDrive') return 'TAXI'
  if (tabId === 'cruise') return 'CRUISE'
  return ''
}

async function loadHotelLocationTips() {
  const keyword = hotelSearchForm.value.destinationKeyword.trim()
  hotelSearchForm.value.selectedLocation = null
  showHotelTips.value = true

  if (keyword.length < 2) {
    hotelLocationTips.value = []
    return
  }

  hotelTipLoading.value = true
  try {
    hotelLocationTips.value = await hotelDebouncedTips(keyword)
  } catch (error) {
    hotelLocationTips.value = []
    if (error.message && error.message.includes('频繁')) {
      lastMessage.value = error.message
    }
  } finally {
    hotelTipLoading.value = false
  }
}

function selectHotelLocation(tip) {
  hotelSearchForm.value.selectedLocation = tip
  hotelSearchForm.value.destinationKeyword = tip.name || tip.cityName || hotelSearchForm.value.destinationKeyword
  showHotelTips.value = false
}

async function loadServiceLocationTips(type) {
  const keyword = (type === 'start' ? searchForm.value.startPlace : searchForm.value.destination).trim()
  if (type === 'start') {
    serviceStartLocation.value = null
    showServiceStartTips.value = true
    if (keyword.length < 2) { serviceStartTips.value = []; return }
    serviceStartTipLoading.value = true
  } else {
    serviceEndLocation.value = null
    showServiceEndTips.value = true
    if (keyword.length < 2) { serviceEndTips.value = []; return }
    serviceEndTipLoading.value = true
  }
  try {
    const tips = await serviceDebouncedTips(keyword)
    if (type === 'start') serviceStartTips.value = tips || []
    else serviceEndTips.value = tips || []
  } catch (error) {
    const msg = error.message || '地点查询失败'
    if (msg.includes('频繁')) lastMessage.value = msg
    if (type === 'start') serviceStartTips.value = []
    else serviceEndTips.value = []
  } finally {
    if (type === 'start') serviceStartTipLoading.value = false
    else serviceEndTipLoading.value = false
  }
}

function selectServiceLocation(type, tip) {
  if (type === 'start') {
    serviceStartLocation.value = tip
    searchForm.value.startPlace = tip.name || searchForm.value.startPlace
    showServiceStartTips.value = false
    return
  }
  serviceEndLocation.value = tip
  searchForm.value.destination = tip.name || searchForm.value.destination
  showServiceEndTips.value = false
}

async function loadChainLocationTips(type) {
  const isStart = type === 'start'
  const keyword = (isStart ? chainStartPlace.value : chainEndPlace.value).trim()
  if (isStart) {
    chainStartLocation.value = null
    showChainStartTips.value = true
    if (keyword.length < 2) { chainStartTips.value = []; return }
    chainStartTipLoading.value = true
  } else {
    chainEndLocation.value = null
    showChainEndTips.value = true
    if (keyword.length < 2) { chainEndTips.value = []; return }
    chainEndTipLoading.value = true
  }
  try {
    const tips = await chainDebouncedTips(keyword)
    if (isStart) chainStartTips.value = tips
    else chainEndTips.value = tips
  } catch (error) {
    if (isStart) chainStartTips.value = []
    else chainEndTips.value = []
  } finally {
    if (isStart) chainStartTipLoading.value = false
    else chainEndTipLoading.value = false
  }
}

function selectChainLocation(type, tip) {
  if (type === 'start') {
    chainStartLocation.value = tip
    chainStartPlace.value = tip.name || chainStartPlace.value
    showChainStartTips.value = false
  } else {
    chainEndLocation.value = tip
    chainEndPlace.value = tip.name || chainEndPlace.value
    showChainEndTips.value = false
  }
}

function hasValidLocation(tip) {
  const parts = String(tip?.location || '').split(',')
  return parts.length === 2 && parts.every(part => Number.isFinite(Number(part)))
}

function assertConfirmedChainLocation(tip, label) {
  if (hasValidLocation(tip)) return true
  lastMessage.value = `请先从高德地点提示中选择${label}，确认经纬度后才能一键规划。`
  window.alert(lastMessage.value)
  if (label === '起始地') showChainStartTips.value = true
  if (label === '终点地') showChainEndTips.value = true
  return false
}

function locationTipText(tip) {
  return [tip.district, tip.address].filter(Boolean).join(' · ') || tip.cityName || tip.adcode || '高德地点'
}

function adjustHotelGuests(field, step) {
  const current = Number(hotelSearchForm.value[field] || 0)
  const min = field === 'adults' || field === 'rooms' ? 1 : 0
  hotelSearchForm.value[field] = Math.max(min, current + step)
}

function validateHotelSearch() {
  if (!hasValidLocation(hotelSearchForm.value.selectedLocation)) {
    lastMessage.value = '请先从高德地点建议中选择目的地，避免地点识别错误。'
    window.alert(lastMessage.value)
    showHotelTips.value = true
    return false
  }
  if (!hotelSearchForm.value.checkInDate || !hotelSearchForm.value.checkOutDate) {
    lastMessage.value = '请选择入住日期和离店日期。'
    window.alert(lastMessage.value)
    return false
  }
  if (hotelSearchForm.value.checkOutDate <= hotelSearchForm.value.checkInDate) {
    lastMessage.value = '离店日期必须晚于入住日期。'
    window.alert(lastMessage.value)
    return false
  }
  return true
}

function validateServiceSearch() {
  if (!hasValidLocation(serviceStartLocation.value)) {
    lastMessage.value = '请先从高德地点建议中选择出发地，确认经纬度后再搜索。'
    window.alert(lastMessage.value)
    showServiceStartTips.value = true
    return false
  }
  if (!hasValidLocation(serviceEndLocation.value)) {
    lastMessage.value = '请先从高德地点建议中选择目的地，确认经纬度后再搜索。'
    window.alert(lastMessage.value)
    showServiceEndTips.value = true
    return false
  }
  return true
}

function pushServicePage(tabId) {
  window.history.pushState({ pageMode: 'searchResults', service: tabId }, '', `/service/${tabId}`)
}

function openServiceSearchPage(tabId, options = {}) {
  const { pushHistory = true } = options
  currentResultTabId.value = tabId
  resultFromSidebar.value = true
  resultType.value = ''
  lastMessage.value = ''
  pageMode.value = 'searchResults'
  hotelDetail.value = null
  if (pushHistory) pushServicePage(tabId)
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    if (shell) shell.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function clearServicePage() {
  window.history.pushState({ pageMode: 'home' }, '', '/')
  destroyLandingMap()
  pageMode.value = 'home'
  resultType.value = ''
  currentResultTabId.value = activeMainTab.value
  hotelDetail.value = null
  resultFromSidebar.value = false
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    const target = document.querySelector('#hero')
    if (shell && target) shell.scrollTo({ top: target.offsetTop, behavior: 'smooth' })
  })
}

function openLandingMapPage(options = {}) {
  const { pushHistory = true } = options
  pageMode.value = 'map'
  resultFromSidebar.value = false
  lastMessage.value = landingMapPoints.value.length
    ? '地图已读取当前行程坐标，可继续查看可视化位置。'
    : '暂无可视化坐标，请先在首页通过高德候选确认起始地和终点地。'
  if (pushHistory) {
    window.history.pushState({ pageMode: 'map' }, '', '/map')
  }
  nextTick(initLandingMap)
}

let landingTempChart = null
let landingHumChart = null

function destroyLandingMap() {
  landingMapOverlays = []
  if (landingTempChart) { landingTempChart.dispose(); landingTempChart = null }
  if (landingHumChart) { landingHumChart.dispose(); landingHumChart = null }
  if (landingMap) {
    landingMap.destroy()
    landingMap = null
  }
}

function initLandingMap() {
  const container = document.getElementById('landing-map-container')
  if (!container) return
  destroyLandingMap()
  if (!window.AMap) {
    landingMapError.value = '地图服务暂不可用，请检查高德地图配置或网络连接。'
    return
  }
  landingMapError.value = ''
  const points = landingMapPoints.value
  const center = points[0]?.position || [116.397428, 39.90923]
  landingMap = new window.AMap.Map(container, {
    center,
    zoom: points.length ? 12 : 5,
    resizeEnable: true,
    viewMode: '2D'
  })
  if (window.AMap.Scale && window.AMap.ToolBar) {
    landingMap.addControl(new window.AMap.Scale())
    landingMap.addControl(new window.AMap.ToolBar({ position: 'RT' }))
  } else if (window.AMap.plugin) {
    window.AMap.plugin(['AMap.Scale', 'AMap.ToolBar'], () => {
      landingMap?.addControl(new window.AMap.Scale())
      landingMap?.addControl(new window.AMap.ToolBar({ position: 'RT' }))
    })
  }
  const markers = points.map(point => new window.AMap.Marker({
    position: point.position,
    title: point.name,
    label: { content: point.label, direction: 'top' }
  }))
  if (markers.length) {
    landingMap.add(markers)
    landingMapOverlays.push(...markers)
    landingMap.setFitView(markers, false, [70, 70, 70, 70])
  }
  const routePaths = extractCurrentRoutePaths()
  if (routePaths.length && window.AMap.Polyline) {
    const colors = ['#2563eb', '#0891b2', '#16a34a', '#f97316', '#7c3aed']
    const polylines = routePaths.map((path, index) => new window.AMap.Polyline({
      path,
      strokeColor: colors[index % colors.length],
      strokeWeight: 6,
      strokeOpacity: 0.82,
      lineJoin: 'round'
    }))
    landingMap.add(polylines)
    landingMapOverlays.push(...polylines)
    landingMap.setFitView([...markers, ...polylines], false, [70, 70, 70, 70])
  }
  // ★ ECharts 天气仪表盘
  nextTick(renderLandingCharts)
}

function renderLandingCharts() {
  const cards = landingMapWeatherCards.value
  if (!cards.length) return
  const weatherData = normalizedChainResult.value?.weatherInfo || chainResult.value?.weatherInfo
  if (!weatherData) return
  const temp = parseFloat(weatherData.temperature) || 0
  const humidity = parseFloat(weatherData.humidity) || 0
  const tempDom = document.getElementById('landing-temp-gauge')
  if (tempDom && !landingTempChart) landingTempChart = echarts.init(tempDom)
  if (landingTempChart) { landingTempChart.setOption({ series: [{ type: 'gauge', startAngle: 210, endAngle: -30, min: -20, max: 50, radius: '90%', center: ['50%', '58%'], axisLine: { lineStyle: { width: 14, color: [[0.3, '#67e0e3'],[0.6, '#37a2da'],[0.8, '#fd666d'],[1, '#e5323e']] } }, pointer: { length: '60%', width: 6, itemStyle: { color: '#334155' } }, detail: { valueAnimation: true, formatter: '{value}℃', fontSize: 16, color: '#0f172a', offsetCenter: [0, '70%'] }, title: { offsetCenter: [0, '32%'], fontSize: 12, color: '#64748b' }, data: [{ value: temp, name: '温度' }] }], grid: { left: 0, right: 0, top: 0, bottom: 0 } }, true) }
  const humDom = document.getElementById('landing-humidity-gauge')
  if (humDom && !landingHumChart) landingHumChart = echarts.init(humDom)
  if (landingHumChart) { landingHumChart.setOption({ series: [{ type: 'gauge', startAngle: 210, endAngle: -30, min: 0, max: 100, radius: '90%', center: ['50%', '58%'], axisLine: { lineStyle: { width: 14, color: [[0.3, '#a5d6a7'],[0.6, '#4caf50'],[0.8, '#ffa726'],[1, '#ef5350']] } }, pointer: { length: '60%', width: 6, itemStyle: { color: '#334155' } }, detail: { valueAnimation: true, formatter: '{value}%', fontSize: 16, color: '#0f172a', offsetCenter: [0, '70%'] }, title: { offsetCenter: [0, '32%'], fontSize: 12, color: '#64748b' }, data: [{ value: humidity, name: '湿度' }] }], grid: { left: 0, right: 0, top: 0, bottom: 0 } }, true) }
}

function parseLngLat(value) {
  if (Array.isArray(value) && value.length >= 2) {
    const lng = Number(value[0])
    const lat = Number(value[1])
    return Number.isFinite(lng) && Number.isFinite(lat) ? [lng, lat] : null
  }
  const parts = String(value || '').split(',').map(item => Number(item.trim()))
  return parts.length >= 2 && parts.every(Number.isFinite) ? [parts[0], parts[1]] : null
}

function pointFromTip(tip, label) {
  const position = parseLngLat(tip?.location)
  if (!position) return null
  return {
    label,
    name: tip.name || tip.address || label,
    address: tip.address || tip.district || tip.cityName || '',
    position
  }
}

function pointFromGeo(geo, label) {
  const position = parseLngLat(geo?.location || [geo?.longitude ?? geo?.lng ?? geo?.lon, geo?.latitude ?? geo?.lat])
  if (!position) return null
  return {
    label,
    name: geo.name || geo.poiName || geo.address || label,
    address: geo.address || geo.city || geo.district || '',
    position
  }
}

function buildLandingMapPointsLegacy(data) {
  const start = pointFromGeo(data?.geoInfo?.start, '起点') || pointFromTip(chainStartLocation.value, '起点')
  const end = pointFromGeo(data?.geoInfo?.end, '终点') || pointFromTip(chainEndLocation.value, '终点')
  return [start, end].filter(Boolean)
}

function extractCurrentRoutePathLegacy() {
  const route = getFirstRoute()
  const rawPath = route.routePolyline || route.polyline || route.path || route.routePath
  if (Array.isArray(rawPath)) {
    return rawPath.map(item => parseLngLat(item?.location || item)).filter(Boolean)
  }
  if (typeof rawPath === 'string') {
    return rawPath.split(';').map(parseLngLat).filter(Boolean)
  }
  return []
}

function buildLandingMapPoints(data) {
  const points = [
    pointFromGeo(data?.geoInfo?.start, '起点') || pointFromTip(chainStartLocation.value, '起点')
  ]
  landingMapSegments.value.forEach((segment, index) => {
    points.push(pointFromGeo(segment.raw?.start || segment.raw?.startPoint || segment.raw?.from, `第${index + 1}段起点`))
    points.push(pointFromGeo(segment.raw?.end || segment.raw?.endPoint || segment.raw?.to, `第${index + 1}段终点`))
  })
  points.push(pointFromGeo(data?.geoInfo?.end, '终点') || pointFromTip(chainEndLocation.value, '终点'))
  const seen = new Set()
  return points.filter(Boolean).filter(point => {
    const key = point.position.join(',')
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function buildLandingMapSegments(data) {
  if (!data || needsClarification(data)) return []
  const firstRoute = data.routePlans?.[0] || {}
  const sourceSegments = firstRoute.segments || firstRoute.tripSegments || data.tripSegments || data.segments || data.routePlans || []
  return sourceSegments.map((segment, index) => {
    const startName = segment.startName || segment.startPlace || segment.startPoint?.name || segment.start?.name || segment.from?.name || segment.from || data.demand?.startPlace || chainStartPlace.value
    const endName = segment.endName || segment.endPlace || segment.endPoint?.name || segment.end?.name || segment.to?.name || segment.to || data.demand?.endPlace || chainEndPlace.value
    return {
      key: segment.id || segment.segmentId || `${index}-${startName}-${endName}`,
      order: index + 1,
      title: segment.title || segment.segmentName || segment.segmentType || `第${index + 1}段行程`,
      startName,
      endName,
      mode: displayValue(segment.transportMode || segment.trafficMode || segment.mode || segment.routeType || firstRoute.routeType, '待确认'),
      duration: formatDuration(segment.duration || segment.durationText || segment.timeCost),
      distance: formatDistance(segment.distance || segment.distanceText),
      note: displayValue(segment.routeDescription || segment.description || segment.instruction || segment.summary, '按当前规划结果展示'),
      raw: segment
    }
  })
}

function buildLandingMapWeatherCards(data) {
  const weather = data?.weatherInfo
  if (!weather) return []
  return [{
    key: weather.city || 'weather',
    city: weather.city || data?.demand?.endPlace || chainEndPlace.value || '目的地',
    weather: weatherDisplay(weather),
    note: weather.reportTime || weather.updateTime || '来自当前规划结果'
  }]
}

function buildLandingMapReminderCards(data) {
  const risks = (data?.riskWarnings || []).map((risk, index) => ({
    key: risk.id || risk.title || `risk-${index}`,
    icon: 'fa-triangle-exclamation',
    title: displayValue(risk.title || risk.riskType, '风险提醒'),
    content: displayValue(risk.content || risk.suggestion || risk.description, '请关注当前行程风险')
  }))
  const timeline = (data?.timelineItems || []).slice(0, 4).map((item, index) => ({
    key: item.id || item.title || `timeline-${index}`,
    icon: 'fa-bell',
    title: displayValue(item.title || item.stage || item.stageType, '行程提醒'),
    content: displayValue(item.content || item.description || item.status, '待开始')
  }))
  return [...risks, ...timeline].slice(0, 6)
}

function formatDistance(value) {
  if (value === null || value === undefined || value === '') return '待确认'
  const number = Number(value)
  if (!Number.isFinite(number)) return String(value)
  if (number > 1000) return `${(number / 1000).toFixed(1)} 公里`
  return `${number} 米`
}

function extractCurrentRoutePaths() {
  const segmentPaths = landingMapSegments.value
    .map(segment => segment.raw?.routePolyline || segment.raw?.polyline || segment.raw?.path || segment.raw?.routePath)
    .map(normalizeRoutePath)
    .filter(path => path.length >= 2)
  if (segmentPaths.length) return segmentPaths
  const path = normalizeRoutePath(getFirstRoute().routePolyline || getFirstRoute().polyline || getFirstRoute().path || getFirstRoute().routePath)
  return path.length >= 2 ? [path] : []
}

function normalizeRoutePath(rawPath) {
  if (Array.isArray(rawPath)) {
    return rawPath.map(item => parseLngLat(item?.location || item)).filter(Boolean)
  }
  if (typeof rawPath === 'string') {
    return rawPath.split(';').map(parseLngLat).filter(Boolean)
  }
  return []
}

function syncPageFromAddress() {
  if (window.location.pathname === '/map') {
    openLandingMapPage({ pushHistory: false })
    return
  }
  const pathMatch = window.location.pathname.match(/^\/service\/([^/]+)$/)
  const service = pathMatch ? pathMatch[1] : new URLSearchParams(window.location.search).get('service')
  if (service && travelTabs.some((tab) => tab.id === service)) {
    openServiceSearchPage(service, { pushHistory: false })
    return
  }
  pageMode.value = 'home'
}

async function handleSearch(tabId = activeMainTab.value, options = {}) {
  const { pushHistory = true, fromSidebar = false } = options
  loading.value = true
  lastMessage.value = ''
  hotelDetail.value = null
  resultFromSidebar.value = fromSidebar
  const targetTabId = tabId || activeMainTab.value
  const targetIsDrive = targetTabId === 'selfDrive'
  currentResultTabId.value = targetTabId
  try {
    if (targetTabId === 'hotel') {
      if (!validateHotelSearch()) {
        return
      }
      resultType.value = 'hotel'
      pageMode.value = 'searchResults'
	      const selected = hotelSearchForm.value.selectedLocation
	      const [longitude, latitude] = String(selected?.location || '').split(',')
	      hotelResults.value = await searchHotels({
	        cityName: selected?.cityName || selected?.district || hotelSearchForm.value.destinationKeyword,
	        adcode: selected?.adcode,
	        longitude,
	        latitude,
	        checkInDate: hotelSearchForm.value.checkInDate,
	        checkOutDate: hotelSearchForm.value.checkOutDate,
	        adults: hotelSearchForm.value.adults,
	        children: hotelSearchForm.value.children,
	        rooms: hotelSearchForm.value.rooms,
	        preference: hotelSearchForm.value.preference,
	        minPrice: hotelSearchForm.value.minPrice,
	        maxPrice: hotelSearchForm.value.maxPrice
	      })
	      trafficResults.value = []
	      resultType.value = 'hotel'
	      lastMessage.value = fromSidebar ? '' : `已查询到 ${hotelResults.value.length} 条住宿结果，优先使用真实酒店 API，失败时自动降级本地数据`
    } else {
      if (fromSidebar && !validateServiceSearch()) {
        return
      }
      resultType.value = 'traffic'
      pageMode.value = 'searchResults'
      const driveSearch = activeDriveTab.value === 'rentalCar'
        ? { startPlace: rentalCarForm.value.pickupPlace, endPlace: rentalCarForm.value.returnPlace === '与取车相同' ? '西湖湖滨' : rentalCarForm.value.returnPlace, date: rentalCarForm.value.date }
        : { startPlace: privateCarForm.value.startPlace, endPlace: privateCarForm.value.endPlace, date: privateCarForm.value.date }
      trafficResults.value = await searchTraffic({
        type: trafficType(targetTabId),
        startPlace: targetIsDrive ? driveSearch.startPlace : searchForm.value.startPlace,
        endPlace: targetIsDrive ? driveSearch.endPlace : searchForm.value.destination,
        departDate: targetIsDrive ? driveSearch.date : searchForm.value.date
      })
      hotelResults.value = []
      resultType.value = 'traffic'
      lastMessage.value = fromSidebar ? '' : `已从数据库查询到 ${trafficResults.value.length} 条交通产品`
    }
    pageMode.value = 'searchResults'
    if (pushHistory) {
      pushServicePage(targetTabId)
    }
    nextTick(() => {
      const shell = document.querySelector('.app-wrapper.landing-mode')
      if (shell) shell.scrollTo({ top: 0, behavior: 'smooth' })
    })
  } catch (error) {
    lastMessage.value = error.message || '搜索失败'
  } finally {
    loading.value = false
  }
}

async function openHotelDetail(hotel) {
  hotelDetail.value = await getHotelDetail(hotel.id)
  pageMode.value = 'hotelDetail'
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    if (shell) shell.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function backToSearchResults() {
  pageMode.value = 'home'
  clearServicePage()
  nextTick(() => {
    const shell = document.querySelector('.app-wrapper.landing-mode')
    const target = document.querySelector('.booking-section')
    if (shell && target) shell.scrollTo({ top: target.offsetTop - 20, behavior: 'smooth' })
  })
}

function openHotelOrder(room) {
  if (!requireLoggedIn('登录后才能下单，订单会绑定到你的账号。')) return
  const hotel = hotelDetail.value.hotel
  orderModal.value = {
    orderType: 'HOTEL',
    productId: room.id,
    productName: `${hotel.name} · ${room.roomName}`,
    startPlace: searchForm.value.startPlace,
    endPlace: hotel.city,
    startTime: `${searchForm.value.date || '2026-05-25'}T14:00:00`,
    endTime: `${searchForm.value.date || '2026-05-25'}T12:00:00`,
    totalAmount: room.price,
    remark: `${room.bedType}，${room.breakfast}，${room.refundable ? '可取消' : '不可取消'}`,
    hotelOrderPayload: {
      userId: currentUserId.value,
      hotelId: String(hotel.id),
      hotelName: hotel.name,
      city: hotel.city,
      address: hotel.address,
      checkInDate: hotelSearchForm.value.checkInDate,
      checkOutDate: hotelSearchForm.value.checkOutDate,
      adults: hotelSearchForm.value.adults,
      children: hotelSearchForm.value.children,
      rooms: hotelSearchForm.value.rooms,
      roomType: room.roomName,
      price: room.price
    }
  }
}

function openTrafficOrder(item) {
  if (!requireLoggedIn('登录后才能下单，订单会绑定到你的账号。')) return
  orderModal.value = {
    orderType: 'TRAFFIC',
    productId: item.id,
    productName: `${item.carrierName} · ${item.seatType}`,
    startPlace: item.startPlace,
    endPlace: item.endPlace,
    startTime: item.departTime,
    endTime: item.arriveTime,
    totalAmount: item.price,
    remark: `${item.type}，${item.duration}，余票 ${item.remainCount}`
  }
}

async function confirmOrder() {
  if (!requireLoggedIn('登录后才能确认订单。')) return
  if (orderModal.value?.orderType === 'HOTEL') {
    const order = await createHotelOrder(orderModal.value.hotelOrderPayload)
    const bookingOrder = await createBookingOrder({
      ...orderModal.value,
      userId: currentUserId.value,
      contactName: currentUserName.value || '出行用户',
      contactPhone: '13800000000'
    })
    orderModal.value = {
      ...orderModal.value,
      id: bookingOrder.id || order.id,
      orderNo: bookingOrder.orderNo || order.orderNo,
      status: bookingOrder.status || order.status,
      created: true
    }
    await loadOrders()
    return
  }
  const payload = {
    ...orderModal.value,
    userId: currentUserId.value,
    contactName: '体验用户',
    contactPhone: '13800000000'
  }
  const order = await createBookingOrder(payload)
  orderModal.value = { ...order, created: true }
  await loadOrders()
}

async function loadOrders() {
  if (!isAuthenticated.value || !currentUserId.value) {
    orders.value = []
    return
  }
  orders.value = await listUserOrders(currentUserId.value)
}

function orderMatchesCategory(order, category) {
  if (category === 'all') return true
  if (category === 'hotel') return order.orderType === 'HOTEL'
  const text = `${order.orderType || ''} ${order.productName || ''} ${order.remark || ''}`.toUpperCase()
  if (category === 'flight') return text.includes('FLIGHT') || text.includes('航班') || text.includes('飞机')
  if (category === 'train') return text.includes('TRAIN') || text.includes('高铁') || text.includes('火车')
  if (category === 'cruise') return text.includes('CRUISE') || text.includes('邮轮') || text.includes('游船')
  return true
}

async function openOrderCategory(categoryId) {
  if (!requireLoggedIn('登录后才能查看自己的历史订单。')) return
  orderFilter.value = categoryId
  showOrderDropdown.value = false
  await loadOrders()
  pageMode.value = 'orders'
  resultType.value = resultType.value || 'orders'
  lastMessage.value = '历史订单已从数据库重新加载'
  scrollToBooking()
}

async function payOrder(order) {
  if (!requireLoggedIn('登录后才能操作订单。')) return
  await payBookingOrder(order.id)
  await loadOrders()
  lastMessage.value = '模拟支付成功，订单状态已同步到数据库'
}

async function cancelOrder(order) {
  if (!requireLoggedIn('登录后才能操作订单。')) return
  await cancelBookingOrder(order.id)
  await loadOrders()
  lastMessage.value = '订单已取消，用户端和管理端会同步显示'
}

function canRequestRefund(order) {
  const type = String(order?.orderType || '').toUpperCase()
  const status = String(order?.status || '').toUpperCase()
  return ['HOTEL', 'TRAIN', 'FLIGHT', 'CRUISE'].includes(type) && ['PAID', 'CONFIRMED'].includes(status)
}

async function requestRefund(order) {
  if (!requireLoggedIn('登录后才能申请退款。')) return
  if (!order?.id) return
  try {
    await createAfterSaleOrder({
      userId: currentUserId.value,
      orderId: order.id,
      serviceType: 'REFUND',
      title: `退款申请：${order.productName || order.orderNo}`,
      description: `用户申请退款，订单号：${order.orderNo || order.id}，订单类型：${order.orderType || '未知'}，金额：${order.totalAmount || 0}`,
      status: 'PENDING'
    })
    lastMessage.value = '退款申请已提交，管理端可在退款申请审核中处理'
    await loadOrders()
    window.alert(lastMessage.value)
  } catch (error) {
    lastMessage.value = error.message || '退款申请提交失败'
    window.alert(lastMessage.value)
  }
}

onMounted(() => {
  loadOrders().catch(() => {
    orders.value = []
  })
  loadHomeRecommendations()
  syncPageFromAddress()
  window.addEventListener('popstate', syncPageFromAddress)
})

watch(() => [props.isLoggedIn, props.loginUser], () => {
  loadOrders().catch(() => {
    orders.value = []
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', syncPageFromAddress)
  destroyLandingMap()
})

async function loadHomeRecommendations() {
  try {
    const [hotels, traffic] = await Promise.all([
      searchHotels({}),
      searchTraffic({})
    ])
    recommendedHotels.value = hotels.slice(0, 4)
    recommendedTraffic.value = traffic.slice(0, 4)
  } catch (error) {
    recommendedHotels.value = []
    recommendedTraffic.value = []
  }
}

function scrollAssistantToBottom() {
  nextTick(() => {
    if (assistantBodyRef.value) assistantBodyRef.value.scrollTop = assistantBodyRef.value.scrollHeight
  })
}

function getAssistantFallback(message) {
  if (message.includes('酒店') || message.includes('住宿')) return '你可以在首页选择“住宿”，输入目的地后搜索，酒店和房型会从后端数据库返回，并能生成模拟订单。'
  if (message.includes('订单')) return '订单会保存到 booking_order 表，用户端“我的订单”和管理端订单列表都会看到同一份数据。'
  return '我可以帮你解释路线、住宿和订单流程。需要完整规划时，可以点击进入智能体系统。'
}

async function sendAssistantMessage() {
  const text = assistantInput.value.trim()
  if (!text || assistantLoading.value) return
  assistantMessages.value.push({ role: 'user', content: text })
  assistantInput.value = ''
  assistantLoading.value = true
  assistantOrder.value = null
  scrollAssistantToBottom()
  try {
    const data = await askAssistant({
      message: text,
      scene: assistantScene.value,
      userId: currentUserId.value,
      from: searchForm.value.startPlace,
      destination: searchForm.value.destination || hotelSearchForm.value.destinationKeyword
    })
    const thought = Array.isArray(data?.thoughtSteps) && data.thoughtSteps.length
      ? `\n\n思考过程：\n${data.thoughtSteps.map((step, index) => `${index + 1}. ${step}`).join('\n')}`
      : ''
    const candidates = Array.isArray(data?.candidates) && data.candidates.length
      ? `\n\n候选资源：\n${data.candidates.map((item, index) => `${index + 1}. ${item.title || item.name || item.route} ${item.route ? `(${item.route})` : ''} ¥${item.price ?? '-'}`).join('\n')}`
      : ''
    assistantMessages.value.push({
      role: 'assistant',
      content: `${data?.reply || getAssistantFallback(text)}${thought}${candidates}`
    })
    if (data?.action === 'BOOKING_PROPOSAL' && data?.orderPayload) {
      assistantOrder.value = { ...data.orderPayload, created: false }
    }
  } catch (error) {
    assistantMessages.value.push({ role: 'assistant', content: `${getAssistantFallback(text)}\n\n接口错误：${error.message}` })
  } finally {
    assistantLoading.value = false
    scrollAssistantToBottom()
  }
}

async function createAssistantOrder() {
  if (!assistantOrder.value || assistantOrder.value.created) return
  if (!requireLoggedIn('登录后才能让智能体创建订单。')) return
  assistantLoading.value = true
  try {
    const order = await createBookingOrder({
      ...assistantOrder.value,
      userId: currentUserId.value,
      contactName: '体验用户',
      contactPhone: '13800000000'
    })
    assistantOrder.value = { ...order, created: true }
    await loadOrders()
    assistantMessages.value.push({ role: 'assistant', content: `订单已写入数据库：${order.orderNo}\n状态：${order.status}\n你可以继续点击“确认订单”完成模拟支付。` })
  } catch (error) {
    assistantMessages.value.push({ role: 'assistant', content: `创建订单失败：${error.message}` })
  } finally {
    assistantLoading.value = false
    scrollAssistantToBottom()
  }
}

async function payAssistantOrder() {
  if (!assistantOrder.value?.id) return
  if (!requireLoggedIn('登录后才能确认订单。')) return
  assistantLoading.value = true
  try {
    const order = await payBookingOrder(assistantOrder.value.id)
    assistantOrder.value = { ...order, created: true }
    await loadOrders()
    assistantMessages.value.push({ role: 'assistant', content: `确认成功，订单 ${order.orderNo} 已完成模拟支付，状态：${order.status}。` })
  } catch (error) {
    assistantMessages.value.push({ role: 'assistant', content: `确认订单失败：${error.message}` })
  } finally {
    assistantLoading.value = false
    scrollAssistantToBottom()
  }
}

async function runTravelChainPlan() {
  const startPlace = chainStartPlace.value.trim()
  const endPlace = chainEndPlace.value.trim()
  const demandText = chainInput.value.trim()
  if (chainLoading.value || chainAnimating.value) return
  if (!startPlace) {
    lastMessage.value = '请先填写或定位起始地'
    window.alert(lastMessage.value)
    return
  }
  if (!endPlace) {
    lastMessage.value = '请先填写终点地'
    window.alert(lastMessage.value)
    return
  }
  if (!assertConfirmedChainLocation(chainStartLocation.value, '起始地')) return
  if (!assertConfirmedChainLocation(chainEndLocation.value, '终点地')) return
  if (!demandText) {
    lastMessage.value = '请输入你的出行需求'
    window.alert(lastMessage.value)
    return
  }
  const message = `从${startPlace}出发去${endPlace}，${demandText}`
  chainLoading.value = true
  chainAnimating.value = true
    chainResult.value = null
    chainOrderPreview.value = null
    resetPlanningStages()
    visiblePlanningStages.value = ['intent']
    planningStageStates.value.intent = 'thinking'
    await nextTick()
    try {
    chainResult.value = await planTravelChain({
      userId: sessionStorage.getItem('userId') || 1,
      sessionId: sessionStorage.getItem('chatSessionId') || `home-${Date.now()}`,
      startPlace,
      endPlace,
      startLocation: chainStartLocation.value.location,
      endLocation: chainEndLocation.value.location,
      startAdcode: chainStartLocation.value.adcode,
      endAdcode: chainEndLocation.value.adcode,
      startCity: chainStartLocation.value.cityName,
      endCity: chainEndLocation.value.cityName,
      message
    })
    lastMessage.value = '一句话智能规划已完成：咨询、规划、匹配、风险、时间轴、报告已闭环生成'
    await loadOrders()
    nextTick(() => {
      const shell = document.querySelector('.app-wrapper.landing-mode')
      const target = document.querySelector('.home-trip-workspace')
      if (shell && target) shell.scrollTo({ top: target.offsetTop - 18, behavior: 'smooth' })
    })
    await playPlanningStages()
  } catch (error) {
    lastMessage.value = error.message || '智能规划失败'
  } finally {
    chainLoading.value = false
    chainAnimating.value = false
  }
}

function clearChainInput() {
  chainInput.value = ''
}

function fillHotDemandFinal(text) {
  const match = text.match(/^从(.+?)(?:去|到|前往)(.+)$/)
  if (match?.[1]) {
    chainStartPlace.value = match[1].trim()
    chainStartLocation.value = null
  }
  if (match?.[2]) {
    chainEndPlace.value = match[2].trim()
    chainEndLocation.value = null
    chainInput.value = '推荐少步行路线和周边服务'
  } else {
    chainInput.value = text
  }
}

function submitChainSupplement() {
  const supplement = chainSupplementInput.value.trim()
  if (!supplement) return
  chainInput.value = `${chainInput.value.trim()}；补充信息：${supplement}`
  chainSupplementInput.value = ''
  runTravelChainPlan()
}

function createInitialPlanningStageStates() {
  return {
    intent: 'waiting',
    route: 'waiting',
    traffic: 'waiting',
    hotel: 'waiting',
    risk: 'waiting',
    timeline: 'waiting',
    order: 'waiting'
  }
}

function resetPlanningStages() {
  planningStageStates.value = createInitialPlanningStageStates()
  visiblePlanningStages.value = []
}

async function playPlanningStages() {
  const data = normalizedChainResult.value
  if (needsClarification(data)) {
    planningStageStates.value.intent = 'need_input'
    return
  }
  const available = availablePlanningStages.value
  for (const key of planningStageOrder) {
    if (!available.includes(key)) continue
    if (!visiblePlanningStages.value.includes(key)) {
      visiblePlanningStages.value.push(key)
    }
    planningStageStates.value[key] = 'thinking'
    await wait(760)
    planningStageStates.value[key] = 'done'
  }
  Object.keys(planningStageStates.value).forEach(key => {
    if (!available.includes(key)) planningStageStates.value[key] = 'skipped'
  })
}

function wait(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

function normalizeChainResult(raw) {
  if (!raw) {
    return {
      complete: true,
      clarifyingQuestion: null,
      missingFields: [],
      demand: {},
      routePlans: [],
      trafficProducts: [],
      hotelMatches: [],
      riskWarnings: [],
      timelineItems: [],
      mockOrderPreview: null,
      reportPreview: null
    }
  }
  const routePlans = raw.routePlans || raw.routePlan?.routes || []
  const hotelMatches = raw.hotelMatches || raw.hotels || []
  const riskWarnings = raw.riskWarnings || raw.risks || []
  const timelineItems = raw.timelineItems || raw.timeline || []
  const trafficProducts = raw.trafficProducts || routePlans
  return {
    complete: raw.complete !== false,
    clarifyingQuestion: raw.clarifyingQuestion || raw.demand?.clarifyingQuestion || null,
    missingFields: raw.missingFields || raw.demand?.missingFields || [],
    demand: raw.demand || {},
      geoInfo: raw.geoInfo || null,
      weatherInfo: raw.weatherInfo || raw.routePlan?.endWeather || null,
      tripSegments: raw.tripSegments || raw.segments || [],
      routePlans,
    trafficProducts,
    hotelMatches,
    riskWarnings,
    timelineItems,
    mockOrderPreview: raw.mockOrderPreview || null,
    reportPreview: raw.reportPreview || raw.report || null
  }
}

function needsClarification(data) {
  return data?.complete === false || Boolean(data?.clarifyingQuestion) || Boolean(data?.missingFields?.length)
}

function buildAvailablePlanningStages(data) {
  if (!data) return []
  if (needsClarification(data)) return ['intent']
  const stages = ['intent']
  if (data.routePlans?.length) stages.push('route')
  if (hasTrafficStage(data)) stages.push('traffic')
  if (data.demand?.hotelRequired === true || data.demand?.needHotel === true || data.hotelMatches?.length) stages.push('hotel')
  stages.push('risk')
  if (data.timelineItems?.length) stages.push('timeline')
  if (hasBookableResources(data)) stages.push('order')
  return stages
}

function hasTrafficStage(data) {
  const routeText = JSON.stringify(data?.routePlans || '').toLowerCase()
  return Boolean(data?.trafficProducts?.length || /car|train|bus|subway|metro|taxi|walk|高铁|公交|地铁|打车|自驾/.test(routeText))
}

function hasBookableResources(data) {
  return Boolean(data?.mockOrderPreview || data?.trafficProducts?.length || data?.hotelMatches?.length)
}

function buildItinerarySummaryCards(data) {
  if (!data || needsClarification(data)) return []
  const route = data.routePlans?.[0] || {}
  const hotel = data.hotelMatches?.[0] || {}
  const risk = data.riskWarnings?.[0] || {}
  const timeline = data.timelineItems?.[0] || {}
  return [
    {
      key: 'route',
      icon: 'fa-route',
      title: route.routeName || '路线方案',
      value: `${displayValue(data.demand?.startPlace || chainStartPlace.value)} → ${displayValue(data.demand?.endPlace || chainEndPlace.value)}`,
      meta: [formatDuration(route.duration || route.durationText || route.timeCost), route.distance ? `${route.distance} 公里` : '', formatMoney(route.cost || route.price || route.priceDesc, '')].filter(Boolean).join(' · ')
    },
    {
      key: 'weather',
      icon: 'fa-cloud-sun',
      title: '目的地天气',
      value: data.weatherInfo ? `${data.weatherInfo.weather || '天气待确认'} ${data.weatherInfo.temperature || ''}℃` : '待确认',
      meta: data.weatherInfo?.city || data.demand?.endPlace || chainEndPlace.value
    },
    {
      key: 'hotel',
      icon: 'fa-hotel',
      title: '住宿匹配',
      value: hotel.name || hotel.hotelName || (data.demand?.needHotel || data.demand?.hotelRequired ? '待确认酒店' : '未选择住宿'),
      meta: hotel.address || hotel.distanceDesc || '按当前规划结果展示'
    },
    {
      key: 'risk',
      icon: 'fa-shield-halved',
      title: '风险提醒',
      value: risk.title || risk.riskType || '暂无高风险提醒',
      meta: risk.content || risk.suggestion || timeline.title || '可继续查看服务时间轴'
    }
  ]
}

function getStageMeta(key) {
  return planningStageMeta.find(item => item.key === key) || planningStageMeta[0]
}

function getFirstRoute() {
  return normalizedChainResult.value.routePlans?.[0] || {}
}

function getFirstHotel() {
  return normalizedChainResult.value.hotelMatches?.[0] || {}
}

function displayValue(value, fallback = '待补充') {
  if (value === null || value === undefined || value === '') return fallback
  const explicitMap = { car: '自驾', train: '高铁', bus: '公交', subway: '地铁', metro: '地铁', taxi: '打车', walk: '步行', min_walk: '少步行', BUSINESS_TRIP: '商务出差', SHORT_TRIP: '短途旅游' }
  const key = String(value)
  return explicitMap[key] || explicitMap[key.toLowerCase()] || explicitMap[key.toUpperCase()] || enumLabelMap[value] || enumLabelMap[key.toUpperCase()] || value
}

function formatDuration(value) {
  if (value === null || value === undefined || value === '') return '待确认'
  const number = Number(value)
  if (Number.isFinite(number)) return `${number} 分钟`
  return value
}

function formatMoney(value, fallback = '待确认') {
  if (value === null || value === undefined || value === '') return fallback
  const number = Number(value)
  if (Number.isFinite(number)) return `¥${number}`
  return value
}

function stageStatusLabel(key) {
  const state = planningStageStates.value[key] || 'waiting'
  return { waiting: '待补充', thinking: '生成中', done: '已完成', skipped: '已跳过', need_input: '待补充' }[state] || '待补充'
}

function shouldShowStage(key) {
  return visiblePlanningStages.value.includes(key) || ['thinking', 'done', 'need_input'].includes(planningStageStates.value[key])
}

function isStageThinking(key) {
  return planningStageStates.value[key] === 'thinking'
}

function buildItineraryIntentFields(data) {
  const demand = data?.demand || {}
  return [
    { icon: 'fa-location-dot', label: '出发地', value: demand.startPlace || chainStartPlace.value },
    { icon: 'fa-flag-checkered', label: '目的地', value: demand.endPlace || chainEndPlace.value },
    { icon: 'fa-route', label: '途经点', value: Array.isArray(demand.waypoints) ? demand.waypoints.join('、') : demand.waypoints },
    { icon: 'fa-calendar-days', label: '出行日期', value: demand.travelDate || demand.date || demand.departureDate },
    { icon: 'fa-user-group', label: '人数', value: demand.peopleCount || demand.people || demand.passengerCount },
    { icon: 'fa-sliders', label: '出行偏好', value: demand.transportPreference || demand.sortRule || demand.preference },
    { icon: 'fa-hotel', label: '酒店需求', value: demand.needHotel === true || demand.hotelRequired === true ? '需要住宿' : demand.needHotel === false || demand.hotelRequired === false ? '不需要住宿' : '待确认' },
    { icon: 'fa-suitcase-rolling', label: '行李情况', value: demand.luggage || demand.luggageInfo },
    { icon: 'fa-wallet', label: '预算偏好', value: demand.budget || demand.budgetPreference },
    { icon: 'fa-location-crosshairs', label: '真实坐标状态', value: landingMapPoints.value.length >= 2 ? '已确认坐标' : '待补充' }
  ]
}

function buildItineraryRouteCards(data) {
  const labels = ['方案 A：推荐方案', '方案 B：快速方案', '方案 C：经济方案']
  return (data?.routePlans || []).slice(0, 3).map((route, index) => ({
    key: route.id || route.routeId || `${index}-${route.routeName || route.routeType || 'route'}`,
    title: labels[index] || `方案 ${index + 1}`,
    summary: route.routeName || route.summary || route.description || `${displayValue(data.demand?.startPlace || chainStartPlace.value)} → ${displayValue(data.demand?.endPlace || chainEndPlace.value)}`,
    mode: displayValue(route.transportMode || route.routeType || route.mode || data.demand?.transportPreference),
    duration: formatDuration(route.duration || route.durationText || route.timeCost),
    price: formatMoney(route.cost || route.price || route.priceDesc),
    tags: splitTags(route.tags || route.advantages || route.reasons || route.strategyName)
  }))
}

function buildItineraryTrafficCards(data) {
  return (data?.trafficProducts || data?.routePlans || []).slice(0, 5).map((item, index) => ({
    key: item.id || item.productId || item.routeId || `traffic-${index}`,
    mode: displayValue(item.transportMode || item.trafficMode || item.type || item.routeType),
    route: `${displayValue(item.startPlace || item.startStation || item.startPoint?.name || item.from || data.demand?.startPlace || chainStartPlace.value)} → ${displayValue(item.endPlace || item.endStation || item.endPoint?.name || item.to || data.demand?.endPlace || chainEndPlace.value)}`,
    name: item.productName || item.trainNo || item.flightNo || item.serviceName || item.routeName || item.carrierName,
    time: item.departureTime && item.arrivalTime ? `${item.departureTime}-${item.arrivalTime}` : item.durationText || formatDuration(item.duration || item.timeCost),
    price: formatMoney(item.price || item.cost || item.totalAmount),
    status: item.bookable === false ? '待确认' : '可预约'
  }))
}

function buildItineraryHotelCards(data) {
  return (data?.hotelMatches || []).slice(0, 4).map((hotel, index) => ({
    key: hotel.id || hotel.hotelId || `hotel-${index}`,
    name: hotel.name || hotel.hotelName,
    address: hotel.address || hotel.distanceDesc || hotel.location,
    score: hotel.score || hotel.rating,
    price: formatMoney(hotel.price || hotel.minPrice || hotel.lowestPrice),
    tags: splitTags(hotel.tags || hotel.reasons || hotel.features)
  }))
}

function buildItineraryRiskCards(data) {
  if (!data?.riskWarnings?.length) {
    return [{ key: 'no-risk', level: '暂无明显风险', title: '当前路线未触发高风险规则', content: '建议继续关注天气、拥堵和出发时间变化。' }]
  }
  return data.riskWarnings.slice(0, 5).map((risk, index) => ({
    key: risk.id || risk.title || `risk-${index}`,
    level: displayValue(risk.level || risk.riskLevel, '待补充'),
    title: displayValue(risk.title || risk.riskType, '风险提醒'),
    content: displayValue(risk.content || risk.suggestion || risk.description, '待补充')
  }))
}

function buildItineraryTimelineGroups(data) {
  const items = data?.timelineItems || []
  const fallback = { before: ['天气提醒', '证件提醒', '行李提醒', '出发建议'], during: ['路线提醒', '换乘提醒', '到站提醒', '风险提醒'], after: ['酒店入住', '售后工单', '服务报告'] }
  return [
    { key: 'before', title: '行前准备', icon: 'fa-clipboard-check' },
    { key: 'during', title: '行中服务', icon: 'fa-route' },
    { key: 'after', title: '行后服务', icon: 'fa-file-circle-check' }
  ].map(group => {
    const matched = items.filter(item => String(item.stageType || item.stage || item.phase || '').toLowerCase().includes(group.key))
    return { ...group, items: (matched.length ? matched.map(item => item.title || item.content || item.description) : fallback[group.key]).slice(0, 4) }
  })
}

function buildItineraryOrderItems(data) {
  const items = []
  if (data?.mockOrderPreview) {
    items.push({ key: data.mockOrderPreview.id || 'preview', name: data.mockOrderPreview.productName || data.mockOrderPreview.orderNo || '模拟预约', price: formatMoney(data.mockOrderPreview.totalAmount || data.mockOrderPreview.price), status: displayValue(data.mockOrderPreview.status, '待确认') })
  }
  if (!items.length && hasBookableResources(data)) {
    items.push({ key: 'resource', name: '可预约交通 / 住宿资源', price: formatMoney(estimateChainTotal(), '待确认'), status: '可预约' })
  }
  return items
}

function splitTags(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(item => displayValue(item)).filter(Boolean).slice(0, 3)
  return String(value).split(/[,，、|]/).map(item => displayValue(item.trim())).filter(Boolean).slice(0, 3)
}

function estimateChainTotal() {
  const route = getFirstRoute()
  const hotel = getFirstHotel()
  return Math.round(Number(route.cost || 0) + Number(hotel.minPrice || hotel.lowestPrice || 0))
}

async function createChainMockOrder() {
  if (chainLoading.value || chainAnimating.value) return
  if (!requireLoggedIn('登录后才能创建模拟预约订单。')) return
  const data = normalizedChainResult.value
  const route = getFirstRoute()
  const hotel = getFirstHotel()
  try {
    chainLoading.value = true
    chainOrderPreview.value = await createBookingOrder({
      userId: currentUserId.value,
      orderType: 'PACKAGE',
      productName: [route.routeName, hotel.name || hotel.hotelName].filter(Boolean).join(' + ') || '智能出行套餐',
      startPlace: data.demand?.startPlace || chainStartPlace.value,
      endPlace: data.demand?.endPlace || '',
      totalAmount: estimateChainTotal() || 1,
      contactName: currentUserName.value || '出行用户',
      contactPhone: '13800000000',
      remark: '首页一键规划生成的模拟预约'
    })
    lastMessage.value = `模拟预约已创建：${chainOrderPreview.value.orderNo || chainOrderPreview.value.id}`
    await loadOrders()
  } catch (error) {
    lastMessage.value = error.message || '模拟预约创建失败'
  } finally {
    chainLoading.value = false
  }
}

async function payChainOrderPreview() {
  if (!chainOrderPreview.value?.id) return
  if (!requireLoggedIn('登录后才能模拟支付订单。')) return
  try {
    chainLoading.value = true
    chainOrderPreview.value = await payBookingOrder(chainOrderPreview.value.id)
    lastMessage.value = `模拟支付完成：${chainOrderPreview.value.orderNo || chainOrderPreview.value.id}`
    await loadOrders()
  } catch (error) {
    lastMessage.value = error.message || '模拟支付失败'
  } finally {
    chainLoading.value = false
  }
}

function markChainReportGenerated() {
  lastMessage.value = normalizedChainResult.value.reportPreview?.reportSummary || '服务报告摘要已生成'
}

function openTripConsultation() {
  if (!hasCurrentItinerary.value) {
    lastMessage.value = '请先生成行程后再咨询'
    window.alert(lastMessage.value)
    return
  }
  const data = normalizedChainResult.value
  assistantScene.value = displayValue(data.demand?.sceneType, '出行咨询')
  assistantInput.value = `请结合当前行程回答我的问题：${displayValue(data.demand?.startPlace || chainStartPlace.value)}到${displayValue(data.demand?.endPlace || chainEndPlace.value)}，${displayValue(data.demand?.transportPreference || data.demand?.sortRule, '智能推荐')}。`
  showAssistant.value = true
  nextTick(() => {
    const input = document.querySelector('.assistant-footer input')
    if (input) input.focus()
  })
}

async function queryTripWeather() {
  const startCity = chainStartLocation.value?.cityName || normalizedChainResult.value.geoInfo?.start?.city || chainStartPlace.value
  const endCity = chainEndLocation.value?.cityName || normalizedChainResult.value.geoInfo?.end?.city || chainEndPlace.value
  if (!String(startCity || '').trim() || !String(endCity || '').trim()) {
    lastMessage.value = '请先填写起点和终点'
    window.alert(lastMessage.value)
    return
  }
  weatherLoading.value = true
  try {
    const [startWeather, endWeather] = await Promise.all([
      getWeather(startCity),
      getWeather(endCity)
    ])
    lastMessage.value = `天气查询：${startCity} ${weatherDisplay(startWeather)}；${endCity} ${weatherDisplay(endWeather)}`
  } catch (error) {
    lastMessage.value = error.message || '天气查询失败'
  } finally {
    weatherLoading.value = false
  }
}

function weatherDisplay(weather) {
  if (!weather) return '待确认'
  return [weather.weather, weather.temperature ? `${weather.temperature}℃` : '', weather.windDirection || weather.winddirection, weather.windPower || weather.windpower].filter(Boolean).join(' ')
}

function importChainPlanToMap() {
  const data = normalizedChainResult.value
  const route = getFirstRoute()
  localStorage.setItem('currentMapPlan', JSON.stringify({
    planId: chainOrderPreview.value?.id || '',
    startPlace: data.demand?.startPlace || chainStartPlace.value,
    endPlace: data.demand?.endPlace || route.endPoint || '',
    waypoints: [],
    routeType: data.demand?.transportPreference || route.routeType || ''
  }))
  pageMode.value = 'searchResults'
  currentResultTabId.value = 'selfDrive'
  resultFromSidebar.value = true
}

function fillHotDemand(text) {
  const match = text.match(/^从(.+?)(?:去|到|前往)(.+)$/)
  if (match?.[1] && !chainStartPlace.value.trim()) {
    chainStartPlace.value = match[1].trim()
    chainStartLocation.value = null
  }
  if (match?.[2]) {
    chainEndPlace.value = match[2].trim()
    chainEndLocation.value = null
    chainInput.value = '推荐少步行路线和周边服务'
  } else {
    chainInput.value = text
  }
}

async function autoLocateStartPlace() {
  if (!navigator.geolocation) {
    lastMessage.value = '定位失败，请手动输入出发地'
    window.alert(lastMessage.value)
    return
  }
  locatingStartPlace.value = true
  navigator.geolocation.getCurrentPosition(
    position => {
      const { longitude, latitude } = position.coords || {}
      reverseGeocodeStartPlace(longitude, latitude)
    },
    () => {
      locatingStartPlace.value = false
      lastMessage.value = '定位失败，请手动输入出发地'
      window.alert(lastMessage.value)
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
  )
}

function reverseGeocodeStartPlace(longitude, latitude) {
  if (!longitude || !latitude || !window.AMap) {
    locatingStartPlace.value = false
    lastMessage.value = '定位失败，请手动输入出发地'
    window.alert(lastMessage.value)
    return
  }
  window.AMap.plugin(['AMap.Geocoder'], () => {
    const geocoder = new window.AMap.Geocoder()
    geocoder.getAddress([longitude, latitude], (status, result) => {
      locatingStartPlace.value = false
      const address = result?.regeocode?.formattedAddress
      if (status === 'complete' && address) {
        chainStartPlace.value = address
        chainStartLocation.value = {
          name: address,
          address,
          location: `${longitude},${latitude}`,
          adcode: result?.regeocode?.addressComponent?.adcode || '',
          cityName: result?.regeocode?.addressComponent?.city || result?.regeocode?.addressComponent?.province || ''
        }
      } else {
        lastMessage.value = '定位失败，请手动输入出发地'
        window.alert(lastMessage.value)
      }
    })
  })
}

async function openAfterSaleFromChain(type = 'ORDER_ERROR') {
  if (afterSaleLoading.value) return
  if (!requireLoggedIn('登录后才能提交售后工单。')) return
  afterSaleLoading.value = true
  try {
    const order = await createAfterSaleOrder({
      userId: currentUserId.value,
      orderId: orders.value[0]?.id || null,
      serviceType: type,
      title: '全过程服务售后工单',
      description: `来源于一句话智能规划：${chainInput.value}`
    })
    lastMessage.value = `售后工单已创建：${order.title}，状态 ${order.status}`
  } catch (error) {
    lastMessage.value = error.message || '售后工单创建失败'
  } finally {
    afterSaleLoading.value = false
  }
}
</script>

<template>
  <main v-if="pageMode === 'hotelDetail' && hotelDetail" class="home-page hotel-detail-page">
    <header class="home-header detail-header">
      <div class="home-nav">
        <button type="button" class="nav-sidebar-toggle" @click="isSceneCollapsed = !isSceneCollapsed">
          <i class="fa-solid fa-bars"></i>
        </button>
        <a class="home-brand" href="#hero" @click.prevent="backToSearchResults">
          <img :src="logoUrl" alt="七维智行" />
          <span>七维智行</span>
        </a>
        <button type="button" class="scene-trigger" @click="backToSearchResults">
          <i class="fa-solid fa-arrow-left"></i>
          返回搜索结果
        </button>
        <div class="top-actions">
          <button type="button" class="login-btn" @click="enterApp">进入系统</button>
        </div>
      </div>
    </header>

    <section class="hotel-detail-hero">
      <div class="hotel-detail-cover">
        <img :src="hotelDetail.hotel.coverImage" :alt="hotelDetail.hotel.name" />
      </div>
      <div class="hotel-detail-summary">
        <div class="crumb">首页 / 住宿 / 酒店详情</div>
        <h1>{{ hotelDetail.hotel.name }}</h1>
        <p class="address">{{ hotelDetail.hotel.address }}</p>
        <div class="detail-badges">
          <span>{{ hotelDetail.hotel.score }} 分</span>
          <span>{{ hotelDetail.hotel.starLevel }} 星</span>
          <span v-for="tag in String(hotelDetail.hotel.tags || '').split(',')" :key="tag">{{ tag }}</span>
        </div>
        <p class="detail-desc">{{ hotelDetail.hotel.description }}</p>
        <div class="detail-service-row">
          <span><i class="fa-solid fa-check"></i> 数据库实时房型</span>
          <span><i class="fa-solid fa-location-dot"></i> 可联动交通规划</span>
          <span><i class="fa-solid fa-receipt"></i> 支持模拟订单</span>
        </div>
        <div class="detail-price">
          <span>今日参考价</span>
          <strong>¥{{ hotelDetail.hotel.minPrice }} 起</strong>
        </div>
      </div>
    </section>

    <section class="hotel-detail-content">
      <aside class="hotel-info-panel">
        <h3>住宿亮点</h3>
        <ul>
          <li>位置与交通信息来自酒店模拟数据库，可与后续交通规划联动。</li>
          <li>房型、库存、价格均从后端接口返回，不再写死在前端。</li>
          <li>预订后生成模拟订单，可在我的订单和管理端同步查看。</li>
        </ul>
        <div class="mini-map-card">
          <i class="fa-solid fa-map-location-dot"></i>
          <strong>{{ hotelDetail.hotel.city }}</strong>
          <span>{{ hotelDetail.hotel.address }}</span>
        </div>
      </aside>

      <div class="room-booking-panel">
        <div class="room-panel-head">
          <div>
            <h2>选择房型</h2>
            <p>价格、库存、早餐和取消政策均来自数据库房型表。</p>
          </div>
          <button type="button" @click="backToSearchResults">返回列表</button>
        </div>
        <article v-for="room in hotelDetail.rooms" :key="room.id" class="detail-room-row">
          <div>
            <h3>{{ room.roomName }}</h3>
            <p>{{ room.bedType }} · {{ room.breakfast }}</p>
            <div class="room-policy-tags">
              <span>{{ room.refundable ? '可免费取消' : '不可取消' }}</span>
              <span>当前库存 {{ room.stock }} 间</span>
              <span>到店支付 / 模拟支付</span>
            </div>
          </div>
          <div class="room-price">
            <strong>¥{{ room.price }}</strong>
            <small>含税费参考</small>
          </div>
          <button type="button" @click="openHotelOrder(room)">预订</button>
        </article>
      </div>
    </section>

    <div v-if="orderModal" class="modal-mask">
      <section class="order-modal">
        <h3>{{ orderModal.created ? '订单创建成功' : '确认订单' }}</h3>
        <p>{{ orderModal.productName }}</p>
        <dl>
          <div><dt>出发/入住</dt><dd>{{ orderModal.startPlace || '-' }}</dd></div>
          <div><dt>到达/目的地</dt><dd>{{ orderModal.endPlace || '-' }}</dd></div>
          <div><dt>金额</dt><dd>¥{{ orderModal.totalAmount }}</dd></div>
          <div><dt>备注</dt><dd>{{ orderModal.remark || '-' }}</dd></div>
        </dl>
        <div class="modal-actions">
          <button v-if="!orderModal.created" type="button" @click="confirmOrder">确认创建订单</button>
          <button v-if="orderModal.created && orderModal.status === 'UNPAID'" type="button" @click="payOrder(orderModal); orderModal = null">模拟支付</button>
          <button type="button" class="light" @click="orderModal = null">关闭</button>
        </div>
      </section>
    </div>
  </main>

  <main v-else-if="pageMode === 'map'" class="home-page landing-map-standalone">
    <header class="landing-map-topbar">
      <a class="home-brand" href="#hero" @click.prevent="clearServicePage">
        <img :src="logoUrl" alt="七维智行" />
        <span>七维智行</span>
      </a>
      <button type="button" @click="clearServicePage">
        <i class="fa-solid fa-arrow-left"></i>
        返回首页
      </button>
    </header>

    <section class="landing-map-page" aria-label="行程地图可视化">
      <header class="landing-map-header">
        <div>
          <span>行程规划可视化</span>
          <h1>多段行程地图</h1>
          <p>{{ lastMessage || '地图只展示已确认经纬度的位置、多段行程轨迹、天气和提醒信息。' }}</p>
        </div>
        <div class="landing-map-actions">
          <button type="button" @click="initLandingMap">
            <i class="fa-solid fa-rotate"></i>
            刷新地图
          </button>
        </div>
      </header>

      <div class="landing-map-visual-grid">
        <section class="landing-map-card">
          <div id="landing-map-container" class="landing-map-container"></div>
          <div v-if="landingMapError" class="landing-map-alert">{{ landingMapError }}</div>
          <div v-else-if="!landingMapPoints.length" class="landing-map-alert">
            暂无可视化坐标，请先在首页从高德地点提示中确认起始地和终点地。
          </div>
        </section>

        <aside class="landing-map-insights">
          <article class="landing-map-info-card">
            <span>坐标状态</span>
            <strong>{{ landingMapPoints.length ? `${landingMapPoints.length} 个已确认位置` : '缺少可用坐标' }}</strong>
            <p>没有经纬度时不会规划或绘制路线。</p>
          </article>
          <article v-for="weather in landingMapWeatherCards" :key="weather.key" class="landing-map-info-card">
            <span>天气提醒</span>
            <strong>{{ weather.city }}</strong>
            <p>{{ weather.weather }} · {{ weather.note }}</p>
          </article>
          <article v-if="!landingMapWeatherCards.length" class="landing-map-info-card">
            <span>天气提醒</span>
            <strong>暂无天气结果</strong>
            <p>生成行程后会展示当前规划结果中的天气信息。</p>
          </article>
          <!-- ★ ECharts 天气可视化仪表盘 -->
          <article v-if="normalizedChainResult.weatherInfo" class="landing-map-info-card chart-card">
            <span>🌡️ 目的地温度</span>
            <div id="landing-temp-gauge" class="landing-gauge-dom"></div>
          </article>
          <article v-if="normalizedChainResult.weatherInfo" class="landing-map-info-card chart-card">
            <span>💧 目的地湿度</span>
            <div id="landing-humidity-gauge" class="landing-gauge-dom"></div>
          </article>
        </aside>
      </div>

      <div class="landing-map-detail-grid">
        <section class="landing-map-panel">
          <div class="landing-map-panel-head">
            <span>多段行程</span>
            <strong>{{ landingMapSegments.length ? `${landingMapSegments.length} 段` : '暂无分段' }}</strong>
          </div>
          <div v-if="landingMapSegments.length" class="landing-segment-list">
            <article v-for="segment in landingMapSegments" :key="segment.key" class="landing-segment-card">
              <b>{{ segment.order }}</b>
              <div>
                <strong>{{ displayValue(segment.title) }}</strong>
                <p>{{ displayValue(segment.startName) }} → {{ displayValue(segment.endName) }}</p>
                <span>{{ segment.mode }} · {{ segment.duration }} · {{ segment.distance }}</span>
                <em>{{ segment.note }}</em>
              </div>
            </article>
          </div>
          <div v-else class="landing-map-empty">暂无多段行程，请先在首页完成一键规划。</div>
        </section>

        <section class="landing-map-panel">
          <div class="landing-map-panel-head">
            <span>提醒与风险</span>
            <strong>{{ landingMapReminderCards.length ? `${landingMapReminderCards.length} 条` : '暂无提醒' }}</strong>
          </div>
          <div v-if="landingMapReminderCards.length" class="landing-reminder-list">
            <article v-for="item in landingMapReminderCards" :key="item.key" class="landing-reminder-card">
              <i :class="['fa-solid', item.icon]"></i>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.content }}</p>
              </div>
            </article>
          </div>
          <div v-else class="landing-map-empty">暂无天气风险或时间轴提醒。</div>
        </section>
      </div>
    </section>
  </main>

  <main v-else class="home-page home-shell" :class="{ 'sidebar-collapsed': isSceneCollapsed, 'result-mode': pageMode === 'searchResults' }">
    <LandingSidebar :entries="sidebarEntries" :is-entry-active="isSidebarEntryActive" @select="handleSidebarEntry" />

    <section v-if="pageMode === 'home'" id="hero" class="home-hero">
      <div class="hero-overlay" :style="{ backgroundImage: `url(${heroBgUrl})` }"></div>

      <HomeHeroHeader
        :logo-url="logoUrl"
        :is-logged-in="props.isLoggedIn"
        :current-user-name="currentUserName"
        @toggle-sidebar="isSceneCollapsed = !isSceneCollapsed"
        @search="handleSearch"
        @login="openLoginModal"
      />

      <div class="hero-content">
        <HeroIntro />

        <section class="hero-chain-search" aria-label="一句话智能规划">
          <form class="hero-chain-form" @submit.prevent="runTravelChainPlan">
            <div class="hero-location-row">
              <label class="hero-field origin-field">
                <span class="field-label">起始地</span>
                <div class="field-control">
                  <i class="fa-solid fa-location-dot field-main-icon"></i>
                  <input
                    v-model="chainStartPlace"
                    placeholder="输入后从高德提示中选择"
                    autocomplete="off"
                    @input="loadChainLocationTips('start')"
                    @focus="showChainStartTips = true"
                  />
                  <button class="field-icon-btn" type="button" :disabled="locatingStartPlace" title="自动定位" @click="autoLocateStartPlace">
                    <i :class="['fa-solid', locatingStartPlace ? 'fa-spinner fa-spin' : 'fa-location-crosshairs']"></i>
                  </button>
                  <button class="field-icon-btn" type="button" title="编辑出发地" @click="$event.currentTarget.closest('label')?.querySelector('input')?.focus()">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                </div>
                <div v-if="showChainStartTips && (chainStartTips.length || chainStartTipLoading || chainStartPlace.trim().length >= 2)" class="location-tip-panel chain-location-tip-panel">
                  <small v-if="chainStartTipLoading">正在查询高德地点...</small>
                  <button v-for="tip in chainStartTips" :key="`start-${tip.name}-${tip.location}`" type="button" @click="selectChainLocation('start', tip)">
                    <strong>{{ tip.name }}</strong>
                    <em>{{ locationTipText(tip) }}</em>
                  </button>
                  <small v-if="!chainStartTipLoading && !chainStartTips.length">未识别到精确地点，请继续输入并从高德候选中选择</small>
                </div>
              </label>
              <label class="hero-field destination-field">
                <span class="field-label">终点地</span>
                <div class="field-control">
                  <i class="fa-solid fa-flag-checkered field-main-icon"></i>
                  <input
                    v-model="chainEndPlace"
                    placeholder="输入后从高德提示中选择"
                    autocomplete="off"
                    @input="loadChainLocationTips('end')"
                    @focus="showChainEndTips = true"
                  />
                  <button v-if="chainEndPlace" class="field-icon-btn clear-btn" type="button" title="清空终点地" @click="chainEndPlace = ''; chainEndLocation = null">
                    <i class="fa-solid fa-xmark"></i>
                  </button>
                </div>
                <div v-if="showChainEndTips && (chainEndTips.length || chainEndTipLoading || chainEndPlace.trim().length >= 2)" class="location-tip-panel chain-location-tip-panel">
                  <small v-if="chainEndTipLoading">正在查询高德地点...</small>
                  <button v-for="tip in chainEndTips" :key="`end-${tip.name}-${tip.location}`" type="button" @click="selectChainLocation('end', tip)">
                    <strong>{{ tip.name }}</strong>
                    <em>{{ locationTipText(tip) }}</em>
                  </button>
                  <small v-if="!chainEndTipLoading && !chainEndTips.length">未识别到精确地点，请继续输入并从高德候选中选择</small>
                </div>
              </label>
            </div>
            <label class="hero-field demand-field">
              <span class="field-label">你的需求</span>
              <div class="field-control demand-control">
                <i class="fa-solid fa-wand-magic-sparkles field-main-icon ai-icon"></i>
                <input v-model="chainInput" placeholder="例如：下午再去杭州东站，两个人，少步行，需要提醒证件和天气" />
                <button v-if="chainInput" class="field-icon-btn clear-btn" type="button" title="清空需求" @click="clearChainInput">
                  <i class="fa-solid fa-xmark"></i>
                </button>
                <button class="hero-plan-btn" type="submit" :disabled="chainLoading">
                  <i class="fa-solid fa-wand-magic-sparkles"></i>
                  {{ chainLoading ? '规划中...' : '一键规划' }}
                </button>
              </div>
            </label>
          </form>
        </section>

        <section v-if="false" class="hero-search-card" aria-label="出行搜索">
          <div class="travel-tabs">
            <button v-for="(tab, index) in travelTabs" :key="tab.id" type="button" :class="{ active: activeMainTab === tab.id }" @click="handleMainTabClick(index)">
              <img class="tab-icon" :src="tab.icon" :alt="tab.name" />
              <span class="tab-label">{{ tab.name }}</span>
            </button>
          </div>
          <div class="search-divider"></div>

          <div class="drive-expand" :class="{ open: isDrive }">
            <div class="drive-inner">
              <div class="drive-tabs">
                <button type="button" :class="{ active: activeDriveTab === 'privateCar' }" @click="activeDriveTab = 'privateCar'">私家车</button>
                <button type="button" :class="{ active: activeDriveTab === 'rentalCar' }" @click="activeDriveTab = 'rentalCar'">租车</button>
              </div>
            </div>
          </div>

          <Transition name="search-swap" mode="out-in">
            <form v-if="isDrive && activeDriveTab === 'rentalCar'" key="homeRentalCar" class="search-form rental-form" @submit.prevent="handleSearch">
              <label class="search-field"><span>取车地点</span><input v-model="rentalCarForm.pickupPlace" placeholder="城市、机场、火车站或门店" /></label>
              <label class="search-field"><span>还车地点</span><input v-model="rentalCarForm.returnPlace" placeholder="与取车相同" /></label>
              <label class="search-field"><span>日期</span><input v-model="rentalCarForm.date" type="date" /></label>
              <label class="search-field"><span>取车时间</span><input v-model="rentalCarForm.pickupTime" type="time" /></label>
              <label class="search-field"><span>还车时间</span><input v-model="rentalCarForm.returnTime" type="time" /></label>
              <button class="search-btn" type="submit">{{ loading ? '搜索中...' : '搜索' }}</button>
            </form>
            <form v-else-if="isDrive && activeDriveTab === 'privateCar'" key="homePrivateCar" class="search-form private-form" @submit.prevent="handleSearch">
              <label class="search-field"><span>出发地</span><input v-model="privateCarForm.startPlace" placeholder="请输入出发地" /></label>
              <label class="search-field"><span>目的地</span><input v-model="privateCarForm.endPlace" placeholder="请输入目的地" /></label>
              <label class="search-field"><span>日期</span><input v-model="privateCarForm.date" type="date" /></label>
              <label class="search-field"><span>出发时间</span><input v-model="privateCarForm.startTime" type="time" /></label>
              <label class="search-field"><span>乘客 / 偏好</span><input v-model="privateCarForm.preference" placeholder="舒适、少拥堵、低费用" /></label>
              <button class="search-btn" type="submit">{{ loading ? '搜索中...' : '搜索' }}</button>
            </form>
            <form v-else-if="activeMainTab === 'hotel'" key="homeHotel" class="search-form hotel-form hotel-commerce-form" @submit.prevent="handleSearch">
              <label class="search-field hotel-location-field">
                <span>目的地城市 / 位置</span>
                <input
                  v-model="hotelSearchForm.destinationKeyword"
                  placeholder="城市、商圈、景区或酒店名"
                  autocomplete="off"
                  @input="loadHotelLocationTips"
                  @focus="showHotelTips = true"
                />
                <div v-if="showHotelTips && (hotelLocationTips.length || hotelTipLoading || lastMessage)" class="location-tip-panel">
                  <small v-if="hotelTipLoading">正在查询高德地点...</small>
                  <button v-for="tip in hotelLocationTips" :key="`${tip.name}-${tip.location}`" type="button" @click="selectHotelLocation(tip)">
                    <strong>{{ tip.name }}</strong>
                    <em>{{ tip.district || tip.address }}</em>
                  </button>
                  <small v-if="!hotelTipLoading && !hotelLocationTips.length">请输入后从高德建议中选择地点</small>
                </div>
              </label>
              <label class="search-field"><span>入住日期</span><input v-model="hotelSearchForm.checkInDate" type="date" /></label>
              <label class="search-field"><span>离店日期</span><input v-model="hotelSearchForm.checkOutDate" type="date" /></label>
              <div class="search-field guest-picker-field">
                <span>旅客与房间</span>
                <button type="button" @click="showGuestPicker = !showGuestPicker">{{ hotelGuestText }}</button>
                <div v-if="showGuestPicker" class="guest-picker-popover">
                  <div><b>成人</b><nav><button type="button" @click="adjustHotelGuests('adults', -1)">-</button><strong>{{ hotelSearchForm.adults }}</strong><button type="button" @click="adjustHotelGuests('adults', 1)">+</button></nav></div>
                  <div><b>儿童</b><nav><button type="button" @click="adjustHotelGuests('children', -1)">-</button><strong>{{ hotelSearchForm.children }}</strong><button type="button" @click="adjustHotelGuests('children', 1)">+</button></nav></div>
                  <div><b>房间</b><nav><button type="button" @click="adjustHotelGuests('rooms', -1)">-</button><strong>{{ hotelSearchForm.rooms }}</strong><button type="button" @click="adjustHotelGuests('rooms', 1)">+</button></nav></div>
                </div>
              </div>
              <label class="search-field"><span>偏好 / 价格</span><input v-model="hotelSearchForm.preference" placeholder="舒适优先、低价优先、评分优先" /></label>
              <button class="search-btn" type="submit">{{ loading ? '搜索中...' : '搜索' }}</button>
            </form>
            <form v-else :key="`home-${activeMainTab}`" class="search-form default-form" @submit.prevent="handleSearch">
              <label class="search-field"><span>出发地</span><input v-model="searchForm.startPlace" placeholder="城市、车站或机场" /></label>
              <label class="search-field"><span>{{ tabConfig.destinationLabel }}</span><input v-model="searchForm.destination" :placeholder="tabConfig.destinationPlaceholder" /></label>
              <label class="search-field"><span>日期</span><input v-model="searchForm.date" type="date" /></label>
              <label class="search-field"><span>{{ tabConfig.peopleLabel }}</span><input v-model="searchForm.people" :placeholder="tabConfig.peoplePlaceholder" /></label>
              <label class="search-field"><span>{{ tabConfig.extraLabel }}</span><input v-model="searchForm.extra" :placeholder="tabConfig.extraPlaceholder" /></label>
              <button class="search-btn" type="submit">{{ loading ? '搜索中...' : '搜索' }}</button>
            </form>
          </Transition>
        </section>
      </div>
    </section>

    <section v-if="pageMode === 'home'" class="home-trip-workspace" aria-label="行程与出行助手">
      <section class="trip-panel my-trip-panel">
        <header class="trip-panel-head">
          <div>
            <span>我的行程</span>
            <h2>{{ hasCurrentItinerary ? '当前规划结果' : '待生成行程' }}</h2>
          </div>
          <button type="button" @click="runTravelChainPlan">
            {{ hasCurrentItinerary ? '重新规划' : '开始规划' }}
          </button>
        </header>

        <div v-if="chainAnimating || hasCurrentItinerary || needsClarification(normalizedChainResult)" class="itinerary-module-grid">
          <article v-if="shouldShowStage('intent')" class="itinerary-module-card stage-intent" :class="[`state-${planningStageStates.intent}`]">
            <header><h3>AI 已识别需求</h3><span>{{ stageStatusLabel('intent') }}</span></header>
            <div v-if="isStageThinking('intent')" class="stage-skeleton"><strong>{{ getStageMeta('intent').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="intent-field-grid">
              <div v-for="field in itineraryIntentFields" :key="field.label"><i :class="['fa-solid', field.icon]"></i><span>{{ field.label }}</span><strong>{{ displayValue(field.value) }}</strong></div>
            </div>
            <div v-if="needsClarification(normalizedChainResult)" class="clarify-card">
              <strong>需要补充信息</strong>
              <p>{{ normalizedChainResult.clarifyingQuestion || '请补充关键信息后继续规划。' }}</p>
              <small>{{ normalizedChainResult.missingFields?.map(item => displayValue(item)).join('、') }}</small>
            </div>
          </article>

          <article v-if="shouldShowStage('route')" class="itinerary-module-card stage-route" :class="[`state-${planningStageStates.route}`]">
            <header><h3>推荐出行方案</h3><span>{{ stageStatusLabel('route') }}</span></header>
            <div v-if="isStageThinking('route')" class="stage-skeleton"><strong>{{ getStageMeta('route').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="route-option-grid">
              <article v-for="route in itineraryRouteCards" :key="route.key" class="route-option-card">
                <b>{{ route.title }}</b><strong>{{ displayValue(route.summary) }}</strong><p>{{ route.mode }} · {{ route.duration }} · {{ route.price }}</p>
                <div><span v-for="tag in route.tags" :key="tag">{{ tag }}</span></div>
                <footer><button type="button">选择方案</button><button type="button" class="light" @click="openLandingMapPage">导入地图查看</button></footer>
              </article>
            </div>
          </article>

          <article v-if="shouldShowStage('risk')" class="itinerary-module-card stage-risk" :class="[`state-${planningStageStates.risk}`]">
            <header><h3>动态风险预警</h3><span>{{ stageStatusLabel('risk') }}</span></header>
            <div v-if="isStageThinking('risk')" class="stage-skeleton"><strong>{{ getStageMeta('risk').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="risk-list"><div v-for="risk in itineraryRiskCards" :key="risk.key"><b>{{ risk.level }}</b><strong>{{ risk.title }}</strong><p>{{ risk.content }}</p></div></div>
          </article>

          <article v-if="shouldShowStage('hotel')" class="itinerary-module-card stage-hotel" :class="[`state-${planningStageStates.hotel}`]">
            <header><h3>智能住宿匹配</h3><span>{{ stageStatusLabel('hotel') }}</span></header>
            <div v-if="isStageThinking('hotel')" class="stage-skeleton"><strong>{{ getStageMeta('hotel').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="hotel-match-strip"><article v-for="hotel in itineraryHotelCards" :key="hotel.key"><strong>{{ displayValue(hotel.name) }}</strong><p>{{ displayValue(hotel.address) }}</p><span>{{ displayValue(hotel.score) }} · {{ hotel.price }}</span><div><em v-for="tag in hotel.tags" :key="tag">{{ tag }}</em></div></article></div>
          </article>

          <article v-if="shouldShowStage('traffic')" class="itinerary-module-card stage-traffic" :class="[`state-${planningStageStates.traffic}`]">
            <header><h3>智能交通产品匹配</h3><span>{{ stageStatusLabel('traffic') }}</span></header>
            <div v-if="isStageThinking('traffic')" class="stage-skeleton"><strong>{{ getStageMeta('traffic').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="traffic-product-list"><div v-for="item in itineraryTrafficCards" :key="item.key"><i class="fa-solid fa-route"></i><strong>{{ displayValue(item.name) }}</strong><span>{{ item.route }}</span><em>{{ item.mode }} · {{ item.time }} · {{ item.price }}</em><b>{{ item.status }}</b></div></div>
          </article>

          <article v-if="shouldShowStage('timeline')" class="itinerary-module-card stage-timeline" :class="[`state-${planningStageStates.timeline}`]">
            <header><h3>全过程服务时间轴</h3><span>{{ stageStatusLabel('timeline') }}</span></header>
            <div v-if="isStageThinking('timeline')" class="stage-skeleton"><strong>{{ getStageMeta('timeline').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="service-timeline-grid"><section v-for="group in itineraryTimelineGroups" :key="group.key"><i :class="['fa-solid', group.icon]"></i><strong>{{ group.title }}</strong><p v-for="item in group.items" :key="item">{{ displayValue(item) }}</p></section></div>
          </article>

          <article v-if="shouldShowStage('order')" class="itinerary-module-card stage-order" :class="[`state-${planningStageStates.order}`]">
            <header><h3>模拟预约 / 一键订购</h3><span>{{ stageStatusLabel('order') }}</span></header>
            <div v-if="isStageThinking('order')" class="stage-skeleton"><strong>{{ getStageMeta('order').thinking }}</strong><i></i><i></i><i></i></div>
            <div v-else class="order-preview-list"><div v-for="item in itineraryOrderItems" :key="item.key"><strong>{{ item.name }}</strong><span>{{ item.price }} · {{ item.status }}</span></div><button type="button" @click="createChainMockOrder">生成模拟预约</button></div>
          </article>
        </div>

        <div v-if="false && (chainLoading || chainAnimating)" class="trip-loading-card">
          <i class="fa-solid fa-wand-magic-sparkles"></i>
          <strong>正在使用现有一键规划链路生成行程</strong>
          <span>请稍候，规划结果会在这里更新。</span>
        </div>

        <div v-if="false && hasCurrentItinerary" class="itinerary-card-grid">
          <article v-for="card in itinerarySummaryCards" :key="card.key" class="itinerary-summary-card">
            <i :class="['fa-solid', card.icon]"></i>
            <div>
              <span>{{ card.title }}</span>
              <strong>{{ displayValue(card.value) }}</strong>
              <em>{{ displayValue(card.meta, '待确认') }}</em>
            </div>
          </article>
          <div v-if="normalizedChainResult.timelineItems?.length" class="itinerary-timeline-card">
            <strong>全过程时间轴</strong>
            <div v-for="(item, index) in normalizedChainResult.timelineItems.slice(0, 4)" :key="item.id || item.title || index">
              <i class="fa-solid fa-circle-check"></i>
              <span>{{ displayValue(item.title || item.content || item.description) }}</span>
              <em>{{ displayValue(item.status || item.stageType || item.stage, '待开始') }}</em>
            </div>
          </div>
        </div>

        <div v-if="!chainAnimating && !hasCurrentItinerary" class="trip-empty-state">
          <i class="fa-solid fa-route"></i>
          <strong>暂无行程计划，请先在上方填写信息并开始规划。</strong>
          <button type="button" @click="runTravelChainPlan">开始规划</button>
        </div>
      </section>

      <TravelAssistantPanel :weather-loading="weatherLoading" @open-consultation="openTripConsultation" @query-weather="queryTripWeather" />
    </section>

    <section v-if="pageMode === 'map'" class="landing-map-page" aria-label="地图可视化">
      <header class="landing-map-header">
        <div>
          <span>地图可视化</span>
          <h1>当前行程地图</h1>
          <p>{{ lastMessage || '地图只展示已确认经纬度的位置与后端返回的路线轨迹。' }}</p>
        </div>
        <div class="landing-map-actions">
          <button type="button" @click="clearServicePage">
            <i class="fa-solid fa-arrow-left"></i>
            返回首页
          </button>
          <button type="button" @click="initLandingMap">
            <i class="fa-solid fa-rotate"></i>
            刷新地图
          </button>
        </div>
      </header>

      <div class="landing-map-layout">
        <section class="landing-map-card">
          <div id="landing-map-container" class="landing-map-container"></div>
          <div v-if="landingMapError" class="landing-map-alert">{{ landingMapError }}</div>
          <div v-else-if="!landingMapPoints.length" class="landing-map-alert">
            暂无可视化坐标，请先在首页从高德地点提示中确认起始地和终点地。
          </div>
        </section>

        <aside class="landing-map-side">
          <article>
            <span>坐标来源</span>
            <strong>{{ hasCurrentItinerary ? '当前规划结果' : '首页已确认地点' }}</strong>
            <p>没有经纬度时不会规划或绘制路线。</p>
          </article>
          <article v-for="point in landingMapPoints" :key="`${point.label}-${point.name}`">
            <span>{{ point.label }}</span>
            <strong>{{ point.name }}</strong>
            <p>{{ point.address || '已确认经纬度' }}</p>
          </article>
          <article v-if="!landingMapPoints.length">
            <span>待确认</span>
            <strong>缺少可用坐标</strong>
            <p>请先通过高德候选选择起始地和终点地。</p>
          </article>
          <!-- ★ ECharts 天气仪表盘 -->
          <article v-if="normalizedChainResult.weatherInfo" class="landing-map-info-card chart-card">
            <span>🌡️ 目的地温度</span>
            <div id="landing-temp-gauge" class="landing-gauge-dom"></div>
          </article>
          <article v-if="normalizedChainResult.weatherInfo" class="landing-map-info-card chart-card">
            <span>💧 目的地湿度</span>
            <div id="landing-humidity-gauge" class="landing-gauge-dom"></div>
          </article>
        </aside>
      </div>
    </section>

    <header v-if="pageMode === 'searchResults'" class="home-header result-header">
      <div class="home-nav result-nav">
        <a class="home-brand" href="#hero" @click.prevent="clearServicePage">
          <img :src="logoUrl" alt="七维智行" />
          <span>七维智行</span>
        </a>
        <nav class="result-category-nav" aria-label="出行业务切换">
          <button type="button" class="home-link-btn" @click="clearServicePage">首页</button>
          <button
            v-for="tab in travelTabs"
            :key="tab.id"
            type="button"
            :class="{ active: currentResultTabId === tab.id }"
            @click="handleSearch(tab.id)"
          >
            {{ tab.name }}
          </button>
        </nav>
        <div v-if="resultFromSidebar" class="top-search">
          <input type="text" placeholder="搜索目的地、酒店、交通或出行服务" />
          <button type="button" @click="handleSearch(currentResultTabId)"><i class="fa-solid fa-magnifying-glass"></i></button>
        </div>
        <div class="top-actions">
          <button v-if="!props.isLoggedIn" type="button" class="login-btn" @click="openLoginModal">登录 / 注册</button>
          <button v-else type="button" class="user-avatar-btn" title="已登录">
            {{ currentUserName.slice(0, 1).toUpperCase() }}
          </button>
        </div>
      </div>
    </header>

    <section v-if="false && pageMode === 'home'" class="home-recommendations">
      <div class="platform-showcase">
        <div class="showcase-copy">
          <span>七维智行精选</span>
          <h2>把酒店、交通和智能助手放进同一个预订链路</h2>
          <p>下方推荐内容均来自后端接口返回的 MySQL 模拟数据，搜索、查看详情、下单和订单管理形成完整闭环。</p>
        </div>
        <div v-if="featuredHotel" class="showcase-card" @click="openHotelDetail(featuredHotel)">
          <img :src="featuredHotel.coverImage" :alt="featuredHotel.name" />
          <div>
            <strong>{{ featuredHotel.name }}</strong>
            <span>{{ featuredHotel.city }} · {{ featuredHotel.score }} 分 · ¥{{ featuredHotel.minPrice }}起</span>
          </div>
        </div>
      </div>

      <div class="city-strip">
        <button v-for="city in destinationCities" :key="city" type="button" @click="searchForm.destination = city; handleSearch()">
          {{ city }}
        </button>
      </div>

      <div class="recommend-grid">
        <div class="recommend-block">
          <div class="recommend-head">
            <h2>热门住宿推荐</h2>
            <span>来自酒店数据库</span>
          </div>
          <div class="recommend-list">
            <article v-for="hotel in recommendedHotels" :key="hotel.id" class="recommend-card" @click="openHotelDetail(hotel)">
              <img :src="hotel.coverImage" :alt="hotel.name" />
              <div>
                <strong>{{ hotel.name }}</strong>
                <span>{{ hotel.city }} · {{ hotel.score }} 分 · ¥{{ hotel.minPrice }}起</span>
              </div>
            </article>
            <div v-if="!recommendedHotels.length" class="recommend-empty">暂无推荐酒店</div>
          </div>
        </div>
        <div class="recommend-block">
          <div class="recommend-head">
            <h2>常用出行产品</h2>
            <span>来自交通产品表</span>
          </div>
          <div class="recommend-list traffic">
            <article v-for="item in recommendedTraffic" :key="item.id" class="recommend-traffic" @click="openTrafficOrder(item)">
              <i class="fa-solid fa-route"></i>
              <div>
                <strong>{{ item.startPlace }} → {{ item.endPlace }}</strong>
                <span>{{ item.carrierName }} · {{ item.duration }} · ¥{{ item.price }}</span>
              </div>
            </article>
            <div v-if="!recommendedTraffic.length" class="recommend-empty">暂无推荐交通</div>
          </div>
        </div>
      </div>
    </section>

    <section
      v-if="pageMode === 'searchResults'"
      class="service-result-hero"
      :style="{ backgroundImage: `linear-gradient(90deg, rgba(15, 23, 42, .68), rgba(15, 23, 42, .24)), url(${resultHeroBg})` }"
    >
      <div v-if="!resultFromSidebar">
        <h1>{{ resultTab.name }}搜索结果</h1>
        <p>{{ lastMessage || '结果来自后端接口和 MySQL 模拟业务数据。' }}</p>
      </div>
      <button v-if="!resultFromSidebar" type="button" class="refresh-result-btn" @click="handleSearch(currentResultTabId)">重新搜索</button>
      <form v-if="resultFromSidebar" class="result-inline-search" @submit.prevent="handleSearch(currentResultTabId, { fromSidebar: true })">
        <label v-if="currentResultTabId !== 'hotel'">
          <span>出发地</span>
          <input
            v-model="searchForm.startPlace"
            placeholder="城市、车站或机场"
            autocomplete="off"
            @input="loadServiceLocationTips('start')"
            @focus="showServiceStartTips = true"
          />
          <div v-if="showServiceStartTips && (serviceStartTips.length || serviceStartTipLoading || searchForm.startPlace.trim().length >= 2)" class="location-tip-panel result-location-tip-panel">
            <small v-if="serviceStartTipLoading">正在查询高德地点...</small>
            <button v-for="tip in serviceStartTips" :key="`service-start-${tip.name}-${tip.location}`" type="button" @click="selectServiceLocation('start', tip)">
              <strong>{{ tip.name }}</strong>
              <em>{{ locationTipText(tip) }}</em>
            </button>
            <small v-if="!serviceStartTipLoading && !serviceStartTips.length">请从高德建议中选择出发地</small>
          </div>
        </label>
        <label>
          <span>{{ currentResultTabId === 'hotel' ? '目的地/位置' : resultTabConfig.destinationLabel }}</span>
          <input
            v-if="currentResultTabId === 'hotel'"
            v-model="hotelSearchForm.destinationKeyword"
            placeholder="杭州、西湖、商圈"
            autocomplete="off"
            @input="loadHotelLocationTips"
            @focus="showHotelTips = true"
          />
          <input
            v-else
            v-model="searchForm.destination"
            :placeholder="resultTabConfig.destinationPlaceholder"
            autocomplete="off"
            @input="loadServiceLocationTips('end')"
            @focus="showServiceEndTips = true"
          />
          <div v-if="currentResultTabId === 'hotel' && showHotelTips && (hotelLocationTips.length || hotelTipLoading || hotelSearchForm.destinationKeyword.trim().length >= 2)" class="location-tip-panel result-location-tip-panel">
            <small v-if="hotelTipLoading">正在查询高德地点...</small>
            <button v-for="tip in hotelLocationTips" :key="`result-hotel-${tip.name}-${tip.location}`" type="button" @click="selectHotelLocation(tip)">
              <strong>{{ tip.name }}</strong>
              <em>{{ locationTipText(tip) }}</em>
            </button>
            <small v-if="!hotelTipLoading && !hotelLocationTips.length">请从高德建议中选择目的地</small>
          </div>
          <div v-if="currentResultTabId !== 'hotel' && showServiceEndTips && (serviceEndTips.length || serviceEndTipLoading || searchForm.destination.trim().length >= 2)" class="location-tip-panel result-location-tip-panel">
            <small v-if="serviceEndTipLoading">正在查询高德地点...</small>
            <button v-for="tip in serviceEndTips" :key="`service-end-${tip.name}-${tip.location}`" type="button" @click="selectServiceLocation('end', tip)">
              <strong>{{ tip.name }}</strong>
              <em>{{ locationTipText(tip) }}</em>
            </button>
            <small v-if="!serviceEndTipLoading && !serviceEndTips.length">请从高德建议中选择目的地</small>
          </div>
        </label>
        <label>
          <span>日期</span>
          <input v-if="currentResultTabId === 'hotel'" v-model="hotelSearchForm.checkInDate" type="date" />
          <input v-else v-model="searchForm.date" type="date" />
        </label>
        <button type="submit">{{ loading ? '搜索中...' : '搜索' }}</button>
      </form>
    </section>

    <section v-if="(pageMode === 'searchResults' && resultType) || pageMode === 'orders'" class="booking-section">
      <div class="booking-toolbar">
        <div>
          <h2>{{ pageMode === 'orders' ? '历史订单' : '搜索结果' }}</h2>
          <p v-if="pageMode === 'orders'">酒店、高铁、飞机、邮轮订单均来自后端订单接口和数据库。</p>
          <p v-else-if="!resultFromSidebar">{{ lastMessage || '搜索、选择、下单、支付都会通过后端接口和数据库完成。' }}</p>
        </div>
        <button v-if="pageMode === 'orders'" type="button" @click="loadOrders">刷新订单</button>
      </div>

      <div v-if="loading && resultType === 'hotel'" class="hotel-list">
        <article v-for="item in 3" :key="item" class="hotel-card hotel-skeleton">
          <i></i><div><b></b><p></p><p></p></div><em></em>
        </article>
      </div>

      <div v-else-if="resultType === 'hotel'" class="hotel-list">
        <article v-for="hotel in hotelResults" :key="hotel.id" class="hotel-card">
          <img :src="hotel.coverImage" :alt="hotel.name" />
          <div>
            <h3>{{ hotel.name }}</h3>
            <p>{{ hotel.address }}</p>
            <div class="tags"><span v-for="tag in String(hotel.tags || '').split(',')" :key="tag">{{ tag }}</span></div>
            <p class="desc">{{ hotel.description }}</p>
          </div>
          <div class="price-box">
            <strong>¥{{ hotel.minPrice }}</strong>
            <span>{{ hotel.score }} 分 · {{ hotel.starLevel }} 星</span>
            <button type="button" @click="openHotelDetail(hotel)">查看房型</button>
          </div>
        </article>
      </div>

      <div v-if="resultType === 'traffic'" class="traffic-list">
        <article v-for="item in trafficResults" :key="item.id" class="traffic-row">
          <div class="time"><strong>{{ item.departTime?.slice(11, 16) }}</strong><span>{{ item.startPlace }}</span></div>
          <div class="line"><span>{{ item.carrierName }}</span><i></i><small>{{ item.duration }}</small></div>
          <div class="time"><strong>{{ item.arriveTime?.slice(11, 16) }}</strong><span>{{ item.endPlace }}</span></div>
          <div class="traffic-meta"><b>¥{{ item.price }}</b><span>{{ item.seatType }} · 余 {{ item.remainCount }}</span></div>
          <button type="button" @click="openTrafficOrder(item)">预订</button>
        </article>
      </div>

      <div v-if="pageMode === 'orders' && orders.length" class="order-panel">
        <div class="order-panel-head">
          <h3>{{ orderPanelTitle }}</h3>
          <nav class="order-filter-tabs" aria-label="历史订单分类">
            <button
              v-for="category in orderCategories"
              :key="category.id"
              type="button"
              :class="{ active: orderFilter === category.id }"
              @click="openOrderCategory(category.id)"
            >
              {{ category.name }}
            </button>
          </nav>
        </div>
        <article v-for="order in filteredOrders" :key="order.id" class="order-row">
          <div><strong>{{ order.productName }}</strong><span>{{ order.orderNo }} · {{ order.orderType }} · {{ order.status }}</span></div>
          <b>¥{{ order.totalAmount }}</b>
          <button v-if="order.status === 'UNPAID'" type="button" @click="payOrder(order)">模拟支付</button>
          <button v-if="order.status === 'UNPAID'" type="button" class="light" @click="cancelOrder(order)">取消</button>
          <button v-if="canRequestRefund(order)" type="button" class="light" @click="requestRefund(order)">申请退款</button>
        </article>
        <div v-if="!filteredOrders.length" class="empty-orders">当前分类暂无订单</div>
      </div>
      <div v-else-if="pageMode === 'orders'" class="order-panel">
        <div class="empty-orders">暂无历史订单，酒店、高铁、飞机、邮轮订单生成后会从数据库同步到这里。</div>
      </div>
    </section>

    <div v-if="orderModal" class="modal-mask">
      <section class="order-modal">
        <h3>{{ orderModal.created ? '订单创建成功' : '确认订单' }}</h3>
        <p>{{ orderModal.productName }}</p>
        <dl>
          <div><dt>出发/入住</dt><dd>{{ orderModal.startPlace || '-' }}</dd></div>
          <div><dt>到达/目的地</dt><dd>{{ orderModal.endPlace || '-' }}</dd></div>
          <div><dt>金额</dt><dd>¥{{ orderModal.totalAmount }}</dd></div>
          <div><dt>备注</dt><dd>{{ orderModal.remark || '-' }}</dd></div>
        </dl>
        <div class="modal-actions">
          <button v-if="!orderModal.created" type="button" @click="confirmOrder">确认创建订单</button>
          <button v-if="orderModal.created && orderModal.status === 'UNPAID'" type="button" @click="payOrder(orderModal); orderModal = null">模拟支付</button>
          <button type="button" class="light" @click="orderModal = null">关闭</button>
        </div>
      </section>
    </div>

    <button type="button" class="assistant-fab" @click="showAssistant = true">
      <img class="assistant-fab-icon" :src="xiaoqiAvatar" alt="小七" />
    </button>

    <section v-if="showAssistant" class="assistant-panel" aria-label="小七 AI 助手">
      <header class="assistant-header">
        <div class="assistant-title">
          <img class="assistant-avatar" :src="xiaoqiAvatar" alt="小七" />
          <div><strong>小七 AI 助手</strong><span>智能出行助手 · 在线</span></div>
        </div>
        <div class="assistant-actions">
          <button type="button" class="assistant-enter" @click="enterApp">进入智能体系统</button>
          <button type="button" class="assistant-close" @click="showAssistant = false">×</button>
        </div>
      </header>
      <div ref="assistantBodyRef" class="assistant-body">
        <div v-for="(message, index) in assistantMessages" :key="index" class="assistant-message" :class="message.role">{{ message.content }}</div>
        <div v-if="assistantLoading" class="assistant-message assistant loading"><span></span><span></span><span></span></div>
      </div>
      <div v-if="assistantOrder" class="assistant-order-card">
        <strong>{{ assistantOrder.productName }}</strong>
        <span>{{ assistantOrder.startPlace }} → {{ assistantOrder.endPlace }} · ¥{{ assistantOrder.totalAmount }}</span>
        <div>
          <button v-if="!assistantOrder.created" type="button" @click="createAssistantOrder">一键生成订单</button>
          <button v-else-if="assistantOrder.status === 'UNPAID'" type="button" @click="payAssistantOrder">确认订单</button>
          <em v-else>订单状态：{{ assistantOrder.status }}</em>
        </div>
      </div>
      <footer class="assistant-footer">
        <input v-model="assistantInput" placeholder="问问小七，例如：给我买一张去杭州的机票" @keydown.enter.prevent="sendAssistantMessage" />
        <button type="button" :disabled="assistantLoading || !assistantInput.trim()" @click="sendAssistantMessage">发送</button>
      </footer>
    </section>
  </main>
</template>

<style>
.home-page { min-height: 100vh; overflow-x: hidden; background: #f8fafc; color: #0f172a; }
.home-shell { --scenario-width: 196px; --scenario-expanded: 196px; padding-left: var(--scenario-width); padding-top: 56px; transition: padding-left .34s cubic-bezier(.22, 1, .36, 1); }
.home-shell.sidebar-collapsed { --scenario-width: 76px; }
.home-shell.result-mode { --scenario-width: 0px; padding-left: 0; padding-top: 0; position: relative; }
.home-shell.result-mode .scenario-sidebar { display: none; }
.scenario-sidebar { position: fixed; inset: 56px auto 0 0; z-index: 70; width: var(--scenario-width); height: calc(100vh - 56px); overflow: hidden; background: rgba(255, 255, 255, .94); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-right: 1px solid rgba(226, 232, 240, .86); box-shadow: 12px 0 34px rgba(15, 23, 42, .04); transition: width .34s cubic-bezier(.22, 1, .36, 1), box-shadow .34s ease; }
.scenario-sidebar-inner { width: var(--scenario-expanded); min-width: var(--scenario-expanded); height: 100%; }
.sidebar-menu { height: 100%; display: grid; grid-auto-rows: max-content; gap: 4px; padding: 20px 13px 24px; }
.sidebar-item { height: 48px; display: flex; align-items: center; gap: 14px; padding: 0 14px; border: 0; border-radius: 14px; background: transparent; color: #1f2937; font: inherit; font-size: 16px; font-weight: 760; white-space: nowrap; cursor: pointer; }
.sidebar-item i { width: 22px; min-width: 22px; text-align: center; color: #0b84ff; font-size: 19px; }
.sidebar-item span { opacity: 1; }
.sidebar-item:hover { color: #0b63ce; }
.sidebar-item.active { color: #0969da; }
.sidebar-divider { height: 1px; margin: 10px 0; background: #e5e7eb; }
.sidebar-divider.push-bottom { margin-top: auto; }
.home-shell.sidebar-collapsed .sidebar-menu { padding-inline: 13px; }
.home-shell.sidebar-collapsed .sidebar-item span { opacity: 0; pointer-events: none; }
.home-hero { position: relative; min-height: 590px; overflow: visible; padding: 18px 0 18px; background: #fff; }
.hero-overlay { position: absolute; inset: 0; z-index: 1; pointer-events: none; background-size: cover; background-position: center; background-repeat: no-repeat; }
.hero-overlay { inset: 18px 36px; border-radius: 22px; overflow: hidden; }
.hero-overlay::after { content: ""; position: absolute; inset: 0; background: linear-gradient(135deg, rgba(15, 23, 42, .58), rgba(37, 99, 235, .32) 46%, rgba(15, 23, 42, .18)); }
.home-header { position: fixed; top: 0; left: 0; right: 0; z-index: 90; height: 56px; background: #fff; border-bottom: 1px solid rgba(226, 232, 240, 0.8); box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04); }
.home-nav { width: 100%; height: 100%; margin: 0; padding: 0 36px 0 0; display: grid; grid-template-columns: 76px auto minmax(260px, 440px) 1fr; align-items: center; gap: 20px; }
.result-nav { grid-template-columns: auto minmax(410px, max-content) minmax(220px, 360px) 1fr; padding-left: 36px; gap: 18px; }
.nav-sidebar-toggle { width: 38px; height: 38px; display: grid; place-items: center; border: 0; border-radius: 10px; background: #f1f5f9; color: #334155; font-size: 19px; cursor: pointer; transition: background-color .2s ease, color .2s ease; }
.home-shell .nav-sidebar-toggle { justify-self: center; }
.nav-sidebar-toggle:hover { background: #eaf4ff; color: #2563eb; }
.result-back-toggle { background: #eef6ff; color: #2563eb; }
.home-brand { display: inline-flex; align-items: center; gap: 12px; color: inherit; text-decoration: none; font-size: 20px; font-weight: 850; white-space: nowrap; }
.home-brand img { width: 42px; height: 42px; border-radius: 13px; object-fit: cover; box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18); }
.top-search { height: 38px; max-width: 440px; display: grid; grid-template-columns: 1fr 54px; border: 1px solid #bfdbfe; border-radius: 12px; overflow: hidden; background: rgba(255, 255, 255, .94); justify-self: start; }
.top-search input { min-width: 0; border: 0; outline: none; padding: 0 16px; background: transparent; color: #1e293b; font: inherit; font-size: 14px; }
.top-search button { border: 0; background: #0b84ff; color: #fff; cursor: pointer; font-size: 18px; }
.top-actions { justify-self: end; display: flex; align-items: center; gap: 22px; grid-column: 4; }
.result-nav .top-actions { grid-column: auto; }
.result-category-nav { display: flex; align-items: center; gap: 22px; min-width: 0; overflow-x: auto; white-space: nowrap; }
.result-category-nav button { position: relative; height: 56px; border: 0; background: transparent; color: #334155; font: inherit; font-weight: 850; cursor: pointer; padding: 0 2px; }
.result-category-nav button::after { content: ""; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 999px; background: #2563eb; opacity: 0; transform: scaleX(.35); transition: opacity .2s ease, transform .22s ease; }
.result-category-nav button:hover, .result-category-nav button.active { color: #2563eb; }
.result-category-nav button.active::after { opacity: 1; transform: scaleX(1); }
.order-nav { position: relative; }
.order-nav-trigger { height: 42px; display: inline-flex; align-items: center; gap: 7px; border: 0; background: transparent; color: #334155; font: inherit; font-weight: 780; cursor: pointer; }
.order-nav-trigger b { min-width: 18px; height: 18px; display: inline-grid; place-items: center; border-radius: 999px; background: #ef4444; color: #fff; font-size: 11px; line-height: 1; }
.order-dropdown { position: absolute; top: calc(100% + 12px); right: 0; width: 210px; padding: 8px; border: 1px solid rgba(226, 232, 240, .95); border-radius: 16px; background: rgba(255, 255, 255, .96); backdrop-filter: blur(18px); box-shadow: 0 22px 54px rgba(15, 23, 42, .14); animation: dropdownIn .18s ease; }
.order-dropdown button { width: 100%; height: 40px; border: 0; border-radius: 11px; padding: 0 12px; text-align: left; background: transparent; color: #334155; font: inherit; font-weight: 720; cursor: pointer; }
.order-dropdown button:hover, .order-dropdown button.active { background: #eff6ff; color: #2563eb; }
@keyframes dropdownIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
.scene-nav { position: relative; width: fit-content; }
.scene-trigger, .login-btn, .user-avatar-btn, .scene-dropdown button, .search-btn, .drive-tabs button { font: inherit; border: 0; cursor: pointer; }
.scene-trigger { height: 42px; display: inline-flex; align-items: center; gap: 8px; padding: 0 14px; border-radius: 999px; background: transparent; color: #334155; font-weight: 750; }
.scene-trigger:hover { background: rgba(255, 255, 255, 0.72); }
.scene-dropdown { position: absolute; top: calc(100% + 10px); left: 0; width: 180px; padding: 8px; border-radius: 14px; background: #fff; border: 1px solid rgba(226, 232, 240, 0.9); box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12); }
.scene-dropdown button { width: 100%; height: 42px; padding: 0 12px; border-radius: 10px; text-align: left; background: transparent; color: #334155; font-weight: 650; }
.scene-dropdown button:hover { background: #f1f5f9; color: #2563eb; }
.login-btn { justify-self: end; height: 38px; padding: 0 16px; border: 1px solid #cbd5e1; border-radius: 7px; background: #fff; color: #0f172a; font-weight: 850; box-shadow: 0 4px 12px rgba(15, 23, 42, .04); }
.login-btn:hover { color: #2563eb; border-color: #93c5fd; background: #f8fbff; }
.user-avatar-btn { justify-self: end; width: 42px; height: 42px; display: inline-grid; place-items: center; border-radius: 50%; background: linear-gradient(135deg, #2563eb, #0ea5e9); color: #fff; font-weight: 900; box-shadow: 0 12px 24px rgba(37, 99, 235, .28); cursor: default; }
.result-header { position: absolute; top: 0; left: 0; right: 0; background: transparent; border-bottom: 0; box-shadow: none; color: #fff; }
.result-header .home-brand,
.result-header .result-category-nav button,
.result-header .order-nav-trigger { color: rgba(255, 255, 255, .9); text-shadow: 0 4px 16px rgba(15, 23, 42, .34); }
.result-header .result-category-nav button:hover,
.result-header .result-category-nav button.active { color: #fff; }
.result-header .result-category-nav button::after { background: #fff; }
.result-header .top-search { border-color: rgba(255, 255, 255, .58); background: rgba(255, 255, 255, .9); box-shadow: 0 10px 26px rgba(15, 23, 42, .12); }
.result-header .login-btn { border-color: rgba(255, 255, 255, .76); background: rgba(255, 255, 255, .94); color: #0f172a; }
.home-link-btn { color: #2563eb !important; font-weight: 900 !important; }
.hero-content { position: relative; z-index: 3; width: 100%; min-height: 554px; padding: 54px 76px 54px; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; text-align: center; }
.hero-copy { width: min(920px, 100%); color: #fff; }
.hero-copy h1 { margin: 0; font-size: clamp(34px, 5vw, 64px); line-height: 1.08; letter-spacing: -.05em; font-weight: 900; text-shadow: 0 8px 28px rgba(15, 23, 42, .42); }
.hero-chain-search {
  width: min(94%, 1480px);
  margin: 24px auto 0;
  padding: 32px 38px 36px;
  border: 1px solid rgba(213, 226, 244, .84);
  border-radius: 30px;
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 24px 58px rgba(15, 35, 75, .18);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}
.hero-chain-form {
  display: grid;
  gap: 24px;
}
.hero-location-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}
.hero-field {
  position: relative;
  min-width: 0;
  display: grid;
  gap: 7px;
  text-align: left;
}
.field-label {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #42526f;
  font-size: 19px;
  line-height: 1;
  font-weight: 900;
}
.field-label::before {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, .12);
}
.field-control {
  height: 72px;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: 0 18px 0 20px;
  border: 1px solid #d6e1f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(24, 61, 111, .07);
  transition: border-color .18s ease, box-shadow .18s ease;
}
.demand-control {
  grid-template-columns: 44px minmax(0, 1fr) auto 300px;
  padding-right: 8px;
}
.demand-field {
  width: 100%;
}
.field-control:focus-within {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, .13), 0 12px 28px rgba(24, 61, 111, .08);
}
.field-main-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #1d63f0;
  font-size: 18px;
}
.field-main-icon.ai-icon {
  background: #2b76ff;
  font-size: 17px;
}
.field-control input {
  min-width: 0;
  width: 100%;
  height: 68px;
  border: 0;
  outline: none;
  background: transparent;
  color: #121b33;
  font: inherit;
  font-size: 20px;
  font-weight: 760;
}
.field-control input::placeholder {
  color: #69788f;
  font-weight: 650;
}
.field-icon-btn {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #60708d;
  cursor: pointer;
  font-size: 18px;
}
.field-icon-btn:hover {
  color: #1769e8;
  background: #edf4ff;
}
.field-icon-btn:disabled {
  opacity: .58;
  cursor: wait;
}
.clear-btn {
  font-size: 16px;
}
.hero-plan-btn {
  width: 100%;
  min-width: 260px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 16px;
  background: #1769e8;
  color: #fff;
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(23, 105, 232, .22);
  transition: background-color .18s ease, transform .18s ease, box-shadow .18s ease;
}
.chain-location-tip-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% - 2px);
  z-index: 30;
  max-height: 230px;
  overflow-y: auto;
}
.hero-plan-btn:hover {
  background: #0f5ed8;
  box-shadow: 0 15px 28px rgba(23, 105, 232, .28);
}
.hero-plan-btn:active {
  transform: translateY(1px);
}
.hero-plan-btn:disabled {
  opacity: .7;
  cursor: not-allowed;
  transform: none;
}
.hero-feature-row { margin: 24px auto 0; display: inline-flex; align-items: center; justify-content: center; gap: 28px; color: rgba(255, 255, 255, .94); text-shadow: 0 5px 18px rgba(15, 23, 42, .36); }
.hero-feature-item { display: inline-flex; align-items: center; gap: 12px; text-align: left; }
.hero-feature-item i { width: 42px; height: 42px; display: grid; place-items: center; border: 2px solid rgba(255, 255, 255, .58); border-radius: 50%; color: #e0f2fe; font-size: 20px; box-shadow: 0 0 22px rgba(14, 165, 233, .28); }
.hero-feature-item strong, .hero-feature-item span { display: block; }
.hero-feature-item strong { font-size: 21px; letter-spacing: .01em; }
.hero-feature-item span { margin-top: 5px; color: rgba(255, 255, 255, .78); font-size: 14px; }
.hero-feature-divider { width: 1px; height: 48px; background: rgba(255, 255, 255, .42); }
.hero-search-card { margin-top: 48px; }
.hero-search-card { width: min(1100px, calc(100% - 150px)); overflow: hidden; border-radius: 18px; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border: 1px solid rgba(255, 255, 255, 0.74); box-shadow: 0 16px 38px rgba(15, 23, 42, 0.16); }
.travel-tabs { --tab-width: 72px; --tab-gap: clamp(16px, 3vw, 42px); display: flex; justify-content: center; align-items: flex-end; gap: var(--tab-gap); padding: 18px 30px 0; overflow-x: auto; }
.travel-tabs button { position: relative; min-width: var(--tab-width); width: var(--tab-width); height: 76px; padding: 6px 4px 14px; border: 0; border-radius: 12px 12px 0 0; background: transparent; color: #0f172a; font: inherit; font-weight: 850; cursor: pointer; display: inline-flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px; transition: color .22s ease; }
.travel-tabs button:hover, .travel-tabs button.active { color: #2563eb; }
.travel-tabs button::after { content: ""; position: absolute; left: 8px; right: 8px; bottom: 0; height: 3px; border-radius: 999px; background: #2563eb; opacity: 0; transform: scaleX(.35); transition: opacity .2s ease, transform .24s ease; }
.travel-tabs button.active::after { opacity: 1; transform: scaleX(1); }
.tab-icon { width: 40px; height: 40px; object-fit: contain; }
.tab-label { font-size: 16px; line-height: 1; }
.search-divider { height: 1px; background: rgba(203, 213, 225, .88); }
.drive-expand { max-height: 0; opacity: 0; transform: translateY(-10px); overflow: hidden; transition: max-height .28s ease, opacity .24s ease, transform .28s ease; }
.drive-expand.open { max-height: 78px; opacity: 1; transform: translateY(0); }
.drive-inner { padding: 12px 18px 0; }
.drive-tabs { display: inline-flex; gap: 24px; }
.drive-tabs button { position: relative; height: 38px; padding: 0 2px; background: transparent; color: #475569; font-weight: 850; }
.drive-tabs button.active { color: #2563eb; }
.drive-tabs button.active::after { content: ""; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 999px; background: #2563eb; }
.search-form { display: grid; grid-template-columns: repeat(5, minmax(140px, 1fr)) 112px; gap: 10px; padding: 14px; }
.search-field { height: 76px; display: flex; flex-direction: column; justify-content: center; gap: 8px; padding: 0 16px; border: 1px solid #cbd5e1; border-radius: 16px; background: rgba(255, 255, 255, .9); }
.search-field span { color: #64748b; font-size: 12px; font-weight: 850; }
.search-field input { width: 100%; border: 0; outline: none; background: transparent; color: #0f172a; font: inherit; font-size: 16px; font-weight: 760; }
.search-field input::placeholder { color: #94a3b8; }
.search-btn { height: 76px; border-radius: 999px; background: #2563eb; color: #fff; font-size: 16px; font-weight: 900; box-shadow: 0 14px 24px rgba(37, 99, 235, .22); transition: transform .18s ease, background-color .18s ease; }
.search-btn:hover { transform: translateY(-2px); background: #1d4ed8; }
.hotel-commerce-form { grid-template-columns: 1.45fr 1fr 1fr 1.15fr 1.05fr 112px; }
.hotel-commerce-form .search-field { position: relative; border-color: #d6c3ad; background: rgba(255, 252, 248, .94); }
.hotel-commerce-form .search-field span { color: #8b5e3c; }
.hotel-commerce-form .search-btn { background: #b7793f; box-shadow: 0 14px 24px rgba(139, 94, 60, .2); }
.hotel-commerce-form .search-btn:hover { background: #8b5e3c; }
.location-tip-panel,
.guest-picker-popover { position: absolute; left: 0; top: calc(100% + 10px); z-index: 25; width: min(360px, 92vw); padding: 8px; border: 1px solid #e2e8f0; border-radius: 16px; background: rgba(255, 255, 255, .98); box-shadow: 0 22px 54px rgba(15, 23, 42, .16); }
.location-tip-panel button { width: 100%; display: grid; gap: 4px; padding: 10px 11px; border: 0; border-radius: 12px; background: transparent; text-align: left; cursor: pointer; }
.location-tip-panel button:hover { background: #fff7ed; }
.location-tip-panel strong { color: #0f172a; font-size: 14px; }
.location-tip-panel em,
.location-tip-panel small { color: #64748b; font-style: normal; font-size: 12px; }
.guest-picker-field > button { border: 0; background: transparent; color: #0f172a; text-align: left; font: inherit; font-size: 16px; font-weight: 760; cursor: pointer; }
.guest-picker-popover { display: grid; gap: 10px; }
.guest-picker-popover div { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.guest-picker-popover nav { display: inline-flex; align-items: center; gap: 10px; }
.guest-picker-popover nav button { width: 28px; height: 28px; border: 1px solid #d6c3ad; border-radius: 50%; background: #fff; color: #8b5e3c; cursor: pointer; }
.hotel-skeleton { overflow: hidden; }
.hotel-skeleton i, .hotel-skeleton b, .hotel-skeleton p, .hotel-skeleton em { display: block; border-radius: 12px; background: linear-gradient(90deg, #e2e8f0, #f8fafc, #e2e8f0); background-size: 200% 100%; animation: skeletonMove 1.2s ease-in-out infinite; }
.hotel-skeleton i { width: 190px; height: 126px; }
.hotel-skeleton b { width: 45%; height: 20px; margin-bottom: 14px; }
.hotel-skeleton p { width: 80%; height: 14px; margin-bottom: 10px; }
.hotel-skeleton em { width: 110px; height: 42px; }
@keyframes skeletonMove { from { background-position: 100% 0; } to { background-position: -100% 0; } }
.search-swap-enter-active, .search-swap-leave-active { transition: opacity .18s ease, transform .18s ease; }
.search-swap-enter-from { opacity: 0; transform: translateX(12px); }
.search-swap-leave-to { opacity: 0; transform: translateX(-12px); }

.home-recommendations { width: min(1180px, calc(100% - 48px)); margin: 30px auto 76px; display: grid; gap: 22px; }
.platform-showcase { display: grid; grid-template-columns: 1.1fr .9fr; gap: 18px; align-items: stretch; padding: 22px; border-radius: 26px; background: linear-gradient(135deg, #eff6ff, #fff); border: 1px solid #dbeafe; box-shadow: 0 18px 44px rgba(15, 23, 42, .08); }
.showcase-copy span { color: #2563eb; font-size: 13px; font-weight: 900; }
.showcase-copy h2 { margin: 8px 0 10px; font-size: 30px; letter-spacing: -.04em; }
.showcase-copy p { max-width: 680px; margin: 0; color: #64748b; line-height: 1.8; }
.showcase-card { display: grid; grid-template-columns: 150px 1fr; gap: 14px; align-items: center; padding: 12px; border-radius: 20px; background: rgba(255, 255, 255, .86); border: 1px solid #e2e8f0; cursor: pointer; }
.showcase-card img { width: 150px; height: 104px; border-radius: 16px; object-fit: cover; }
.showcase-card strong, .showcase-card span { display: block; }
.showcase-card span { margin-top: 8px; color: #64748b; font-size: 13px; }
.city-strip { display: flex; gap: 10px; overflow-x: auto; padding: 2px 0; }
.city-strip button { height: 38px; padding: 0 18px; border: 1px solid #cbd5e1; border-radius: 999px; background: #fff; color: #334155; font: inherit; font-weight: 800; cursor: pointer; }
.city-strip button:hover { border-color: #2563eb; color: #2563eb; }
.recommend-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.recommend-block { padding: 18px; border: 1px solid #e2e8f0; border-radius: 22px; background: #fff; box-shadow: 0 12px 34px rgba(15, 23, 42, .06); }
.recommend-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.recommend-head h2 { margin: 0; font-size: 22px; }
.recommend-head span { color: #64748b; font-size: 13px; }
.recommend-list { display: grid; gap: 12px; }
.recommend-card { display: grid; grid-template-columns: 92px 1fr; gap: 12px; align-items: center; padding: 10px; border-radius: 16px; background: #f8fafc; cursor: pointer; transition: transform .18s ease, background-color .18s ease; }
.recommend-card:hover, .recommend-traffic:hover { transform: translateY(-2px); background: #eff6ff; }
.recommend-card img { width: 92px; height: 68px; border-radius: 12px; object-fit: cover; }
.recommend-card strong, .recommend-card span, .recommend-traffic strong, .recommend-traffic span { display: block; }
.recommend-card span, .recommend-traffic span { margin-top: 6px; color: #64748b; font-size: 13px; }
.recommend-traffic { display: grid; grid-template-columns: 42px 1fr; gap: 12px; align-items: center; padding: 13px; border-radius: 16px; background: #f8fafc; cursor: pointer; transition: transform .18s ease, background-color .18s ease; }
.recommend-traffic i { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 14px; background: #eaf4ff; color: #2563eb; }
.recommend-empty { padding: 18px; border-radius: 16px; background: #f8fafc; color: #64748b; text-align: center; font-weight: 760; }
.home-trip-workspace { width: min(1560px, calc(100% - 56px)); margin: 18px auto 34px; display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(340px, .8fr); gap: 18px; align-items: stretch; }
.trip-panel { border: 1px solid rgba(191, 219, 254, .82); border-radius: 18px; background: #fff; box-shadow: 0 16px 38px rgba(15, 35, 75, .08); }
.my-trip-panel { padding: 22px; }
.travel-assistant-panel { padding: 22px; }
.trip-panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.trip-panel-head span { color: #2563eb; font-size: 13px; font-weight: 900; }
.trip-panel-head h2 { margin: 6px 0 0; color: #0f172a; font-size: 24px; letter-spacing: 0; }
.trip-panel-head button, .trip-empty-state button { height: 40px; border: 0; border-radius: 12px; padding: 0 16px; background: #2563eb; color: #fff; font: inherit; font-weight: 900; cursor: pointer; box-shadow: 0 10px 22px rgba(37, 99, 235, .18); }
.itinerary-card-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.itinerary-summary-card { min-height: 118px; padding: 16px; display: flex; gap: 13px; border: 1px solid #dbeafe; border-radius: 18px; background: linear-gradient(180deg, #f8fbff, #fff); box-shadow: 0 8px 22px rgba(15, 35, 75, .05); }
.itinerary-summary-card > i { width: 40px; height: 40px; min-width: 40px; display: grid; place-items: center; border-radius: 14px; color: #2563eb; background: #eaf4ff; }
.itinerary-summary-card div { min-width: 0; display: grid; gap: 6px; align-content: start; }
.itinerary-summary-card span { color: #64748b; font-size: 13px; font-weight: 850; }
.itinerary-summary-card strong { min-width: 0; color: #0f172a; font-size: 17px; line-height: 1.35; word-break: break-word; }
.itinerary-summary-card em { color: #475569; font-size: 13px; font-style: normal; line-height: 1.5; word-break: break-word; }
.itinerary-timeline-card { grid-column: 1 / -1; padding: 16px; display: grid; gap: 10px; border: 1px solid #dbeafe; border-radius: 18px; background: #f8fbff; }
.itinerary-timeline-card > strong { color: #0f172a; font-size: 18px; }
.itinerary-timeline-card div { display: grid; grid-template-columns: 22px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid #e2e8f0; color: #334155; }
.itinerary-timeline-card div:first-of-type { border-top: 0; }
.itinerary-timeline-card i { color: #2563eb; }
.itinerary-timeline-card span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 780; }
.itinerary-timeline-card em { color: #64748b; font-size: 13px; font-style: normal; }
.trip-empty-state, .trip-loading-card { min-height: 260px; display: grid; place-items: center; align-content: center; gap: 12px; border: 1px dashed #bfdbfe; border-radius: 18px; background: #f8fbff; color: #475569; text-align: center; }
.trip-empty-state i, .trip-loading-card i { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 18px; color: #2563eb; background: #eaf4ff; font-size: 22px; }
.trip-empty-state strong, .trip-loading-card strong { color: #0f172a; font-size: 18px; }
.trip-loading-card span { color: #64748b; }
.assistant-entry-list { display: grid; gap: 14px; }
.assistant-entry-list button { min-height: 118px; padding: 18px; display: grid; grid-template-columns: 46px minmax(0, 1fr); grid-template-rows: auto auto; column-gap: 14px; row-gap: 6px; align-items: center; text-align: left; border: 1px solid #dbeafe; border-radius: 18px; background: linear-gradient(180deg, #f8fbff, #fff); color: #0f172a; font: inherit; cursor: pointer; box-shadow: 0 8px 22px rgba(15, 35, 75, .05); }
.assistant-entry-list button:disabled { opacity: .72; cursor: wait; }
.assistant-entry-list i { grid-row: 1 / 3; width: 46px; height: 46px; display: grid; place-items: center; border-radius: 16px; color: #fff; background: #2563eb; font-size: 20px; }
.assistant-entry-list strong { font-size: 18px; }
.assistant-entry-list span { color: #64748b; font-size: 13px; line-height: 1.5; }
.itinerary-module-grid { display: grid; grid-template-columns: 1fr; gap: 18px; align-items: stretch; }
.itinerary-module-card { min-height: auto; padding: 20px; border: 1px solid #e5eaf3; border-radius: 18px; background: #fff; box-shadow: 0 14px 34px rgba(15, 35, 75, .07); animation: moduleFadeIn .28s ease both; }
.itinerary-module-card header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 16px; }
.itinerary-module-card h3 { margin: 0; color: #14213d; font-size: 20px; line-height: 1.2; }
.itinerary-module-card header span { height: 28px; padding: 0 10px; display: inline-flex; align-items: center; border-radius: 999px; background: #eef6ff; color: #2563eb; font-size: 12px; font-weight: 900; white-space: nowrap; }
.itinerary-module-card.state-thinking header span { background: #fff7ed; color: #ea580c; }
.itinerary-module-card.state-done header span { background: #dcfce7; color: #16a34a; }
.itinerary-module-card.state-need_input header span { background: #fef3c7; color: #b45309; }
.stage-order { grid-column: auto; min-height: 160px; }
.stage-skeleton { display: grid; gap: 12px; }
.stage-skeleton strong { color: #64748b; font-size: 14px; }
.stage-skeleton i { height: 18px; border-radius: 999px; background: linear-gradient(90deg, #eef2f7 0%, #f8fbff 42%, #e8eef7 78%); background-size: 220% 100%; animation: shimmer 1s linear infinite; }
.stage-skeleton i:nth-child(3) { width: 78%; }
.stage-skeleton i:nth-child(4) { width: 62%; }
.intent-field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.intent-field-grid div { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 2px 9px; align-items: center; }
.intent-field-grid i { grid-row: 1 / 3; width: 28px; height: 28px; display: grid; place-items: center; border-radius: 10px; background: #eff6ff; color: #2563eb; }
.intent-field-grid span, .route-option-card p, .traffic-product-list span, .hotel-match-strip p, .risk-list p, .service-timeline-grid p, .order-preview-list span { color: #64748b; font-size: 13px; line-height: 1.55; }
.intent-field-grid strong { min-width: 0; color: #0f172a; font-size: 14px; word-break: break-word; }
.route-option-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.route-option-card { min-height: 190px; padding: 14px; display: grid; gap: 9px; border: 1px solid #dbeafe; border-radius: 16px; background: #f8fbff; }
.route-option-card b { color: #2563eb; font-size: 14px; }
.route-option-card strong, .traffic-product-list strong, .hotel-match-strip strong, .risk-list strong, .service-timeline-grid strong, .order-preview-list strong { color: #0f172a; font-size: 15px; line-height: 1.35; }
.route-option-card div, .hotel-match-strip div { display: flex; flex-wrap: wrap; gap: 6px; }
.route-option-card span, .hotel-match-strip em { padding: 4px 8px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-style: normal; font-weight: 800; }
.route-option-card footer { align-self: end; display: grid; grid-template-columns: 1fr; gap: 8px; }
.route-option-card button, .order-preview-list button { height: 34px; border: 0; border-radius: 10px; background: #2563eb; color: #fff; font: inherit; font-size: 13px; font-weight: 900; cursor: pointer; }
.route-option-card button.light { background: #fff; color: #2563eb; border: 1px solid #bfdbfe; }
.risk-list, .traffic-product-list, .order-preview-list { display: grid; gap: 10px; }
.risk-list div, .traffic-product-list div, .order-preview-list div { padding: 12px; border-radius: 14px; background: #f8fafc; border: 1px solid #e5eaf3; }
.risk-list b, .traffic-product-list b { float: right; padding: 4px 8px; border-radius: 999px; background: #fff7ed; color: #ea580c; font-size: 12px; }
.hotel-match-strip { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.hotel-match-strip article { min-height: 124px; padding: 14px; border: 1px solid #e5eaf3; border-radius: 16px; background: #f8fbff; }
.traffic-product-list div { display: grid; grid-template-columns: 32px minmax(0, 1fr) auto; gap: 4px 10px; align-items: center; }
.traffic-product-list i { grid-row: 1 / 4; width: 32px; height: 32px; display: grid; place-items: center; border-radius: 12px; background: #eff6ff; color: #2563eb; }
.traffic-product-list span, .traffic-product-list em { grid-column: 2 / 3; font-style: normal; }
.service-timeline-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.service-timeline-grid section { padding: 14px; border-left: 3px solid #2563eb; border-radius: 14px; background: #f8fbff; }
.service-timeline-grid i { color: #2563eb; margin-bottom: 8px; }
@keyframes shimmer { to { background-position: -220% 0; } }
@keyframes moduleFadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.chain-planner-section { width: min(1180px, calc(100% - 64px)); margin: 36px auto 0; padding: 24px; border: 1px solid #dbeafe; border-radius: 22px; background: linear-gradient(180deg, #f8fbff, #fff); box-shadow: 0 20px 48px rgba(37, 99, 235, .1); }
.chain-planner-head { display: flex; justify-content: space-between; gap: 22px; align-items: flex-start; }
.chain-planner-head span { color: #2563eb; font-weight: 900; font-size: 13px; }
.chain-planner-head h2 { margin: 8px 0; color: #0f172a; font-size: 28px; letter-spacing: 0; }
.chain-planner-head p { max-width: 760px; margin: 0; color: #64748b; line-height: 1.75; }
.chain-planner-head button, .after-sale-actions button { border: 0; border-radius: 12px; background: #2563eb; color: #fff; font: inherit; font-weight: 900; cursor: pointer; }
.chain-planner-head button { min-width: 112px; height: 42px; }
.chain-planner-head button:disabled { opacity: .55; cursor: not-allowed; }
.chain-input-row { margin-top: 18px; }
.chain-input-row input { width: 100%; height: 52px; padding: 0 16px; border: 1px solid #bfdbfe; border-radius: 16px; background: #fff; color: #0f172a; font: inherit; font-weight: 760; outline: none; }
.chain-flow-grid { margin-top: 18px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.chain-card { min-height: 150px; padding: 16px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; box-shadow: 0 12px 28px rgba(15, 23, 42, .06); }
.chain-card strong { display: block; margin-bottom: 12px; color: #0f172a; font-size: 16px; }
.chain-card p, .chain-card small { color: #64748b; line-height: 1.65; }
.chain-card dl { display: grid; gap: 8px; margin: 0; }
.chain-card dl div { display: flex; justify-content: space-between; gap: 12px; }
.chain-card dt { color: #64748b; font-size: 12px; font-weight: 800; }
.chain-card dd { margin: 0; color: #0f172a; font-weight: 850; text-align: right; }
.risk-pill { margin: 8px 0; display: flex; align-items: center; gap: 8px; color: #334155; }
.risk-pill b { width: 72px; height: 26px; display: inline-grid; place-items: center; border-radius: 999px; color: #fff; font-size: 11px; }
.risk-pill.low b { background: #16a34a; }
.risk-pill.medium b { background: #d97706; }
.risk-pill.high b { background: #dc2626; }
.timeline-card ol { margin: 0; padding: 0; display: grid; gap: 8px; list-style: none; }
.timeline-card li { display: flex; align-items: center; gap: 8px; color: #334155; }
.timeline-card em { min-width: 58px; border-radius: 999px; background: #eff6ff; color: #2563eb; text-align: center; font-size: 11px; font-style: normal; font-weight: 900; }
.after-sale-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.after-sale-actions button { height: 32px; padding: 0 12px; font-size: 12px; background: #0f172a; }
.chain-progress-section { width: min(1560px, calc(100% - 56px)); margin: 18px auto 34px; padding: 0; border: 0; background: transparent; box-shadow: none; }
.chain-stage-grid { display: grid; grid-template-columns: 1.08fr 1.15fr 1.08fr; gap: 14px; align-items: stretch; }
.chain-stage-card { min-height: 210px; padding: 18px; border: 1px solid rgba(191, 219, 254, .78); border-radius: 18px; background: rgba(255, 255, 255, .96); box-shadow: 0 12px 32px rgba(15, 35, 75, .08); overflow: hidden; }
.chain-stage-card.stage-intent { order: 1; }
.chain-stage-card.stage-route { order: 2; }
.chain-stage-card.stage-risk { order: 3; }
.chain-stage-card.stage-hotel { order: 4; }
.chain-stage-card.stage-traffic { order: 5; }
.chain-stage-card.stage-timeline { order: 6; }
.chain-stage-card.stage-order { order: 7; }
.chain-stage-card.stage-hotel, .chain-stage-card.stage-traffic, .chain-stage-card.stage-timeline { min-height: 238px; }
.chain-stage-card.stage-hotel { grid-column: span 1; }
.chain-stage-card.stage-order { grid-column: 1 / -1; min-height: auto; }
.stage-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.stage-card-head h3 { margin: 0; color: #0f172a; font-size: 19px; letter-spacing: 0; }
.stage-card-head span { min-width: 62px; height: 24px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: #dcfce7; color: #16a34a; font-size: 12px; font-weight: 900; }
.stage-card-head span.need-input { background: #fff7ed; color: #f97316; }
.stage-thinking-copy { margin: 0 0 14px; color: #64748b; font-size: 14px; font-weight: 760; }
.stage-skeleton { display: grid; gap: 10px; }
.stage-skeleton i { height: 18px; border-radius: 999px; background: linear-gradient(90deg, #eff6ff 0%, #dbeafe 42%, #eff6ff 82%); background-size: 220% 100%; animation: chainShimmer 1.15s linear infinite; }
.stage-skeleton i:nth-child(2) { width: 82%; }
.stage-skeleton i:nth-child(3) { width: 58%; }
.intent-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px 16px; }
.intent-grid > div:not(.intent-note):not(.clarify-card) { display: grid; grid-template-columns: 28px 1fr; gap: 4px 8px; align-items: center; }
.intent-grid i { width: 28px; height: 28px; grid-row: span 2; display: grid; place-items: center; border-radius: 50%; color: #2563eb; background: #eff6ff; }
.intent-grid span { color: #64748b; font-size: 12px; font-weight: 800; }
.intent-grid strong { color: #0f172a; font-size: 14px; font-weight: 900; }
.intent-note, .clarify-card { grid-column: 1 / -1; padding: 12px 14px; border-radius: 14px; background: #eff6ff; color: #334155; }
.intent-note { display: flex; align-items: center; gap: 10px; }
.intent-note i { flex: 0 0 28px; }
.clarify-card { display: grid; gap: 10px; background: #fff7ed; border: 1px solid #fed7aa; }
.clarify-card p { margin: 0; color: #92400e; line-height: 1.65; }
.clarify-card > div { display: grid; grid-template-columns: 1fr auto; gap: 10px; }
.clarify-card input { min-width: 0; height: 40px; padding: 0 12px; border: 1px solid #fed7aa; border-radius: 12px; outline: none; font: inherit; }
.clarify-card button { height: 40px; padding: 0 15px; border: 0; border-radius: 12px; background: #2563eb; color: #fff; font: inherit; font-weight: 900; cursor: pointer; }
.route-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.route-options article { padding: 14px; display: grid; gap: 8px; border: 1px solid #dbeafe; border-radius: 16px; background: #f8fbff; }
.route-options article.recommended { border-color: #60a5fa; box-shadow: inset 0 0 0 1px #bfdbfe; }
.route-options b { width: fit-content; padding: 5px 9px; border-radius: 999px; color: #2563eb; background: #dbeafe; font-size: 12px; }
.route-options strong { color: #0f172a; font-size: 15px; }
.route-options span { color: #475569; font-size: 13px; display: inline-flex; gap: 6px; align-items: center; }
.route-options button { height: 34px; border: 1px solid #bfdbfe; border-radius: 10px; background: #2563eb; color: #fff; font: inherit; font-size: 12px; font-weight: 900; cursor: pointer; }
.traffic-match-list, .risk-list, .service-timeline { display: grid; gap: 10px; }
.traffic-match-list div, .risk-list div, .service-timeline div { min-width: 0; display: grid; grid-template-columns: 38px 1fr auto; gap: 4px 10px; align-items: center; padding: 10px; border-radius: 14px; background: #f8fafc; }
.traffic-match-list i, .risk-list i, .service-timeline i { width: 38px; height: 38px; grid-row: span 2; display: grid; place-items: center; border-radius: 14px; color: #2563eb; background: #eff6ff; }
.traffic-match-list strong, .risk-list strong, .service-timeline strong { color: #0f172a; font-size: 14px; }
.traffic-match-list span, .risk-list span, .service-timeline span { color: #64748b; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.traffic-match-list em, .risk-list em, .service-timeline em { grid-row: span 2; color: #f97316; font-size: 12px; font-style: normal; font-weight: 900; }
.risk-list div.low em { color: #16a34a; }
.risk-list div.high em { color: #dc2626; }
.hotel-match-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.hotel-match-grid article { position: relative; display: grid; gap: 6px; padding: 10px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; }
.hotel-match-grid img { width: 100%; height: 86px; border-radius: 10px; object-fit: cover; background: #eff6ff; }
.hotel-match-grid b { position: absolute; left: 14px; top: 14px; width: 24px; height: 24px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: #f97316; }
.hotel-match-grid strong { color: #0f172a; font-size: 14px; }
.hotel-match-grid span { color: #64748b; font-size: 12px; }
.hotel-match-grid em { color: #f97316; font-size: 12px; font-style: normal; font-weight: 900; }
.chain-order-panel { display: grid; grid-template-columns: 1fr 1fr 180px auto; gap: 14px; align-items: center; }
.order-line { min-height: 66px; padding: 12px 14px; display: grid; gap: 5px; border-radius: 14px; background: #f8fafc; }
.order-line span, .order-total span { color: #64748b; font-size: 12px; font-weight: 800; }
.order-line strong { color: #0f172a; font-size: 14px; }
.order-line em, .order-total strong { color: #f97316; font-style: normal; font-weight: 950; }
.order-total { min-height: 66px; padding: 12px 14px; display: grid; align-content: center; gap: 4px; border-left: 1px solid #e2e8f0; }
.order-total strong { font-size: 28px; line-height: 1; }
.chain-order-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.chain-order-actions button { height: 40px; padding: 0 18px; border: 0; border-radius: 12px; background: #2563eb; color: #fff; font: inherit; font-size: 13px; font-weight: 900; cursor: pointer; }
.chain-order-actions button.pay { background: #c47a34; }
.chain-order-actions button.light { border: 1px solid #cbd5e1; background: #fff; color: #334155; }
.chain-order-actions button:disabled { opacity: .48; cursor: not-allowed; }
.stage-fade-enter-active, .stage-fade-leave-active { transition: opacity .24s ease, transform .24s ease; }
.stage-fade-enter-from, .stage-fade-leave-to { opacity: 0; transform: translateY(12px); }
@keyframes chainShimmer { to { background-position: -220% 0; } }
.service-result-hero { position: relative; width: 100%; min-height: 300px; margin: 0; display: flex; justify-content: space-between; align-items: flex-end; gap: 18px; padding: 112px max(48px, calc((100vw - 1180px) / 2)) 48px; border-radius: 0 0 30px 30px; background-size: cover; background-position: center 52%; background-repeat: no-repeat; color: #fff; box-shadow: 0 20px 48px rgba(15, 23, 42, .18); }
.service-result-hero h1 { margin: 12px 0 8px; font-size: 34px; letter-spacing: -.04em; }
.service-result-hero p { margin: 0; color: rgba(255, 255, 255, .88); text-shadow: 0 3px 14px rgba(15, 23, 42, .34); }
.back-home-btn, .refresh-result-btn { border: 0; border-radius: 999px; font: inherit; font-weight: 850; cursor: pointer; }
.back-home-btn { height: 34px; padding: 0 12px; color: #1d4ed8; background: rgba(255, 255, 255, .92); }
.refresh-result-btn { height: 42px; padding: 0 18px; color: #1d4ed8; background: #fff; }
.result-inline-search { position: absolute; left: 50%; bottom: 26px; transform: translateX(-50%); width: min(900px, calc(100% - 80px)); padding: 14px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)) auto; gap: 10px; border-radius: 18px; background: rgba(255, 255, 255, .94); box-shadow: 0 20px 46px rgba(15, 23, 42, .22); }
.result-inline-search label { position: relative; min-width: 0; height: 58px; padding: 8px 12px; display: flex; flex-direction: column; justify-content: center; border: 1px solid #e2e8f0; border-radius: 12px; background: #fff; }
.result-inline-search span { color: #64748b; font-size: 12px; font-weight: 800; }
.result-inline-search input { min-width: 0; border: 0; outline: 0; color: #0f172a; font: inherit; font-weight: 800; background: transparent; }
.result-inline-search button { width: 92px; border: 0; border-radius: 14px; color: #fff; background: #2563eb; font: inherit; font-weight: 900; cursor: pointer; }
.result-location-tip-panel { top: calc(100% + 8px); width: 100%; max-height: 260px; overflow: auto; }
.result-inline-search .result-location-tip-panel button { width: 100%; display: grid; gap: 4px; padding: 10px 11px; border-radius: 12px; color: inherit; background: transparent; text-align: left; font-weight: 700; }
.result-inline-search .result-location-tip-panel button:hover { background: #eff6ff; }
.service-search-panel { width: min(1180px, calc(100% - 48px)); margin: -14px auto 0; position: relative; z-index: 2; border-radius: 22px; background: #fff; border: 1px solid #e2e8f0; box-shadow: 0 18px 46px rgba(15, 23, 42, .1); overflow: hidden; }
.service-tabs { display: flex; align-items: center; justify-content: center; gap: 26px; padding: 14px 22px 0; border-bottom: 1px solid #e2e8f0; overflow-x: auto; }
.service-tabs button { position: relative; min-width: 72px; height: 74px; border: 0; background: transparent; color: #0f172a; font: inherit; font-weight: 850; cursor: pointer; display: inline-flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; }
.service-tabs button::after { content: ""; position: absolute; left: 8px; right: 8px; bottom: 0; height: 3px; border-radius: 999px; background: #2563eb; opacity: 0; transform: scaleX(.4); transition: opacity .2s ease, transform .22s ease; }
.service-tabs button.active { color: #2563eb; }
.service-tabs button.active::after { opacity: 1; transform: scaleX(1); }
.service-tabs img { width: 34px; height: 34px; object-fit: contain; }

.booking-section { width: min(1180px, calc(100% - 48px)); margin: 28px auto 72px; }
.booking-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.booking-toolbar h2 { margin: 0; font-size: 24px; }
.booking-toolbar p { margin: 6px 0 0; color: #64748b; }
.booking-toolbar button, .hotel-card button, .room-row button, .traffic-row button, .order-row button, .modal-actions button { border: 0; border-radius: 999px; padding: 10px 16px; background: #2563eb; color: #fff; font-weight: 850; cursor: pointer; }
.hotel-list, .traffic-list, .order-panel, .room-panel { display: grid; gap: 12px; margin-top: 14px; }
.hotel-card { display: grid; grid-template-columns: 190px 1fr 150px; gap: 18px; padding: 14px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgba(15, 23, 42, .06); }
.hotel-card img { width: 190px; height: 126px; border-radius: 14px; object-fit: cover; }
.hotel-card h3, .room-panel h3, .order-panel h3 { margin: 0 0 8px; }
.order-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.order-panel-head h3 { margin: 0; }
.order-filter-tabs { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.order-filter-tabs button { height: 34px; padding: 0 13px; border: 1px solid #dbeafe; border-radius: 999px; background: #fff; color: #334155; font: inherit; font-size: 13px; font-weight: 850; cursor: pointer; }
.order-filter-tabs button:hover, .order-filter-tabs button.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; }
.hotel-card p { margin: 0 0 8px; color: #64748b; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.tags span { padding: 4px 8px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 800; }
.desc { font-size: 13px; }
.price-box { display: grid; align-content: center; justify-items: end; gap: 8px; }
.price-box strong { color: #f97316; font-size: 24px; }
.price-box span { color: #64748b; font-size: 13px; }
.room-panel, .order-panel { padding: 16px; border-radius: 18px; background: #fff; border: 1px solid #e2e8f0; }
.room-row, .order-row, .traffic-row { display: grid; grid-template-columns: 1fr auto auto auto auto; align-items: center; gap: 14px; padding: 12px; border-radius: 14px; background: #f8fafc; }
.room-row span, .order-row span { display: block; margin-top: 4px; color: #64748b; font-size: 13px; }
.traffic-row { grid-template-columns: 120px 1fr 120px 150px auto; background: #fff; border: 1px solid #e2e8f0; }
.time strong { display: block; font-size: 20px; }
.time span, .traffic-meta span { display: block; color: #64748b; font-size: 13px; }
.line { display: grid; gap: 6px; text-align: center; color: #334155; }
.line i { height: 2px; background: #cbd5e1; border-radius: 999px; }
.traffic-meta b, .order-row b, .room-row b { color: #f97316; }
.order-row .light, .modal-actions .light { background: #e2e8f0; color: #334155; }
.empty-orders { padding: 18px; border-radius: 14px; background: #f8fafc; color: #64748b; text-align: center; font-weight: 760; }

/* 酒店详情页：保持真实住宿平台的信息层级，所有房型数据仍来自后端酒店详情接口。 */
.hotel-detail-page { min-height: 100vh; padding-top: 56px; background: linear-gradient(180deg, #f3f7ff 0%, #f8fafc 38%, #fff 100%); }
.detail-header { background: rgba(255, 255, 255, .92); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); }
.hotel-detail-hero { width: min(1180px, calc(100% - 48px)); margin: 28px auto 22px; display: grid; grid-template-columns: minmax(420px, .95fr) 1.05fr; gap: 20px; align-items: stretch; }
.hotel-detail-cover { position: relative; min-height: 380px; border-radius: 28px; overflow: hidden; box-shadow: 0 24px 64px rgba(15, 23, 42, .16); background: #dbeafe; }
.hotel-detail-cover::after { content: "精选住宿"; position: absolute; left: 18px; top: 18px; height: 34px; display: inline-flex; align-items: center; padding: 0 13px; border-radius: 999px; color: #fff; background: rgba(15, 23, 42, .48); backdrop-filter: blur(12px); font-size: 13px; font-weight: 850; }
.hotel-detail-cover img { width: 100%; height: 100%; display: block; object-fit: cover; transform: scale(1.01); }
.hotel-detail-summary { display: flex; flex-direction: column; justify-content: center; min-height: 380px; padding: 34px; border: 1px solid #dbeafe; border-radius: 28px; background: rgba(255, 255, 255, .96); box-shadow: 0 18px 48px rgba(15, 23, 42, .08); }
.crumb { width: fit-content; margin-bottom: 16px; padding: 7px 12px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 13px; font-weight: 850; }
.hotel-detail-summary h1 { margin: 0; color: #0f172a; font-size: clamp(30px, 3.4vw, 46px); line-height: 1.12; letter-spacing: -.045em; }
.hotel-detail-summary .address { margin: 14px 0 0; color: #475569; font-size: 16px; line-height: 1.7; }
.detail-badges { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
.detail-badges span { display: inline-flex; align-items: center; height: 30px; padding: 0 11px; border-radius: 999px; color: #1d4ed8; background: #eff6ff; font-size: 13px; font-weight: 850; }
.detail-badges span:first-child { color: #fff; background: #2563eb; }
.detail-desc { margin: 18px 0 0; color: #64748b; line-height: 1.85; }
.detail-service-row { display: flex; flex-wrap: wrap; gap: 10px 16px; margin-top: 20px; padding-top: 18px; border-top: 1px solid #e2e8f0; color: #334155; font-size: 13px; font-weight: 820; }
.detail-service-row span { display: inline-flex; align-items: center; gap: 7px; }
.detail-service-row i { color: #2563eb; }
.detail-price { margin-top: 26px; display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; padding: 18px; border-radius: 20px; background: linear-gradient(135deg, #fff7ed, #fff); border: 1px solid #fed7aa; }
.detail-price span { color: #92400e; font-size: 13px; font-weight: 850; }
.detail-price strong { color: #f97316; font-size: 32px; line-height: 1; }
.hotel-detail-content { width: min(1180px, calc(100% - 48px)); margin: 0 auto 78px; display: grid; grid-template-columns: 310px 1fr; gap: 20px; align-items: start; }
.hotel-info-panel, .room-booking-panel { border: 1px solid #e2e8f0; border-radius: 24px; background: #fff; box-shadow: 0 14px 38px rgba(15, 23, 42, .07); }
.hotel-info-panel { position: sticky; top: 76px; padding: 22px; }
.hotel-info-panel h3 { margin: 0 0 14px; font-size: 20px; }
.hotel-info-panel ul { margin: 0; padding-left: 18px; color: #475569; line-height: 1.85; font-size: 14px; }
.mini-map-card { margin-top: 18px; padding: 18px; border-radius: 20px; background: linear-gradient(135deg, #eff6ff, #f8fafc); border: 1px solid #dbeafe; display: grid; gap: 8px; }
.mini-map-card i { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 15px; color: #2563eb; background: #dbeafe; }
.mini-map-card strong { font-size: 18px; }
.mini-map-card span { color: #64748b; line-height: 1.6; font-size: 13px; }
.room-booking-panel { padding: 20px; }
.room-panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; padding: 4px 4px 18px; border-bottom: 1px solid #e2e8f0; }
.room-panel-head h2 { margin: 0; font-size: 26px; letter-spacing: -.03em; }
.room-panel-head p { margin: 7px 0 0; color: #64748b; }
.room-panel-head button { height: 38px; border: 1px solid #cbd5e1; border-radius: 999px; padding: 0 15px; background: #fff; color: #334155; font: inherit; font-weight: 850; cursor: pointer; }
.detail-room-row { display: grid; grid-template-columns: 1fr 130px 96px; align-items: center; gap: 18px; padding: 20px 4px; border-bottom: 1px solid #e2e8f0; }
.detail-room-row:last-child { border-bottom: 0; }
.detail-room-row h3 { margin: 0; font-size: 20px; }
.detail-room-row p { margin: 8px 0 0; color: #475569; }
.room-policy-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.room-policy-tags span { height: 26px; display: inline-flex; align-items: center; padding: 0 9px; border-radius: 999px; background: #f1f5f9; color: #334155; font-size: 12px; font-weight: 760; }
.room-policy-tags span:first-child { color: #15803d; background: #dcfce7; }
.room-price { text-align: right; }
.room-price strong { display: block; color: #f97316; font-size: 27px; line-height: 1; }
.room-price small { display: block; margin-top: 7px; color: #94a3b8; }
.detail-room-row > button { height: 42px; border: 0; border-radius: 999px; background: #2563eb; color: #fff; font: inherit; font-weight: 900; cursor: pointer; box-shadow: 0 12px 24px rgba(37, 99, 235, .22); }
.detail-room-row > button:hover { background: #1d4ed8; }

.landing-map-standalone { min-height: 100vh; padding: 76px 0 48px; background: #f4f8ff; }
.landing-map-topbar { position: fixed; top: 0; left: 0; right: 0; z-index: 90; height: 58px; padding: 0 34px; display: flex; align-items: center; justify-content: space-between; background: rgba(255, 255, 255, .96); border-bottom: 1px solid #dbeafe; box-shadow: 0 10px 28px rgba(15, 23, 42, .06); }
.landing-map-topbar button { height: 38px; padding: 0 14px; display: inline-flex; align-items: center; gap: 8px; border: 1px solid #bfdbfe; border-radius: 12px; background: #fff; color: #1d4ed8; font: inherit; font-weight: 900; cursor: pointer; }
.landing-map-page { width: min(100% - 72px, 1380px); margin: 0 auto; display: grid; gap: 20px; }
.landing-map-header { min-height: 132px; padding: 26px 28px; display: flex; align-items: center; justify-content: space-between; gap: 20px; border-radius: 18px; background: linear-gradient(135deg, #eff6ff, #ffffff); border: 1px solid #dbeafe; box-shadow: 0 18px 48px rgba(15, 23, 42, .08); }
.landing-map-header span { color: #2563eb; font-size: 13px; font-weight: 900; }
.landing-map-header h1 { margin: 6px 0 8px; color: #0f172a; font-size: 30px; line-height: 1.15; }
.landing-map-header p { margin: 0; color: #475569; line-height: 1.7; }
.landing-map-actions { display: flex; flex-wrap: wrap; gap: 10px; justify-content: flex-end; }
.landing-map-actions button { height: 42px; padding: 0 16px; display: inline-flex; align-items: center; gap: 8px; border: 0; border-radius: 13px; background: #2563eb; color: #fff; font: inherit; font-weight: 900; cursor: pointer; box-shadow: 0 12px 24px rgba(37, 99, 235, .2); }
.landing-map-actions button:first-child { background: #fff; color: #1d4ed8; border: 1px solid #bfdbfe; box-shadow: none; }
.landing-map-visual-grid { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 20px; align-items: stretch; }
.landing-map-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; align-items: stretch; }
.landing-map-card, .landing-map-side article, .landing-map-info-card, .landing-map-panel { border-radius: 18px; background: #fff; border: 1px solid #e2e8f0; box-shadow: 0 18px 48px rgba(15, 23, 42, .08); }
.landing-map-card { position: relative; min-height: 610px; overflow: hidden; }
.landing-map-container { width: 100%; height: 610px; background: #eef6ff; }
.landing-map-alert { position: absolute; left: 22px; right: 22px; bottom: 22px; padding: 14px 16px; border-radius: 14px; background: rgba(255, 255, 255, .94); border: 1px solid #bfdbfe; color: #1e40af; font-weight: 850; box-shadow: 0 16px 40px rgba(15, 23, 42, .12); }
.landing-map-side { display: grid; gap: 14px; align-content: start; }
.landing-map-side article { padding: 18px; display: grid; gap: 8px; }
.landing-map-side span { color: #2563eb; font-size: 12px; font-weight: 900; }
.landing-map-side strong { color: #0f172a; font-size: 17px; }
.landing-map-side p { margin: 0; color: #64748b; line-height: 1.6; }
.landing-map-insights { display: grid; gap: 14px; align-content: start; }
.landing-map-info-card { padding: 18px; display: grid; gap: 8px; }
.landing-map-info-card span, .landing-map-panel-head span { color: #2563eb; font-size: 12px; font-weight: 900; }
.landing-map-info-card strong { color: #0f172a; font-size: 18px; }
.landing-map-info-card p { margin: 0; color: #64748b; line-height: 1.65; }
.landing-gauge-dom { width: 100%; height: 160px; }
.landing-map-info-card.chart-card { background: linear-gradient(135deg, #eff6ff, #f0f9ff); border: 1px solid #bfdbfe; border-radius: 14px; }
.landing-map-detail-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(320px, .75fr); gap: 20px; align-items: start; }
.landing-map-panel { padding: 20px; display: grid; gap: 16px; }
.landing-map-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.landing-map-panel-head strong { color: #0f172a; font-size: 18px; }
.landing-segment-list, .landing-reminder-list { display: grid; gap: 12px; }
.landing-segment-card { padding: 14px; display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 12px; border: 1px solid #dbeafe; border-radius: 16px; background: #f8fbff; }
.landing-segment-card b { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 12px; background: #2563eb; color: #fff; }
.landing-segment-card strong, .landing-reminder-card strong { display: block; color: #0f172a; font-size: 16px; }
.landing-segment-card p, .landing-reminder-card p { margin: 6px 0; color: #334155; line-height: 1.55; }
.landing-segment-card span, .landing-segment-card em { display: block; color: #64748b; font-size: 13px; font-style: normal; line-height: 1.55; }
.landing-reminder-card { padding: 14px; display: grid; grid-template-columns: 38px minmax(0, 1fr); gap: 12px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; }
.landing-reminder-card i { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 14px; background: #eff6ff; color: #2563eb; }
.landing-map-empty { padding: 18px; border-radius: 16px; background: #f8fafc; color: #64748b; font-weight: 850; }

.modal-mask { position: fixed; inset: 0; z-index: 80; display: grid; place-items: center; background: rgba(15, 23, 42, .36); }
.order-modal { width: min(460px, calc(100% - 32px)); padding: 24px; border-radius: 20px; background: #fff; box-shadow: 0 30px 80px rgba(15, 23, 42, .28); }
.order-modal h3 { margin: 0 0 8px; }
.order-modal dl { display: grid; gap: 10px; margin: 18px 0; }
.order-modal dl div { display: flex; justify-content: space-between; gap: 18px; padding-bottom: 8px; border-bottom: 1px solid #e2e8f0; }
.order-modal dt { color: #64748b; }
.order-modal dd { margin: 0; font-weight: 850; text-align: right; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }

.assistant-fab { position: fixed; right: 28px; bottom: 28px; z-index: 60; width: 76px; height: 76px; padding: 0; display: grid; place-items: center; border: 0; border-radius: 50%; background: transparent; box-shadow: 0 18px 38px rgba(37, 99, 235, .32); cursor: pointer; }
.assistant-fab-icon { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; background: #fff; border: 3px solid rgba(255, 255, 255, .82); }
.assistant-panel { position: fixed; right: 28px; bottom: 28px; z-index: 61; width: min(40vw, 680px); min-width: 520px; height: min(620px, calc((100vh - 56px) * .66)); display: flex; flex-direction: column; overflow: hidden; border-radius: 18px; background: rgba(255, 255, 255, .94); backdrop-filter: blur(18px); border: 1px solid rgba(255, 255, 255, .72); box-shadow: 0 26px 70px rgba(15, 23, 42, .24); }
.assistant-header { min-height: 72px; padding: 12px 14px; display: grid; grid-template-columns: minmax(150px, 1fr) auto; align-items: center; gap: 12px; border-bottom: 1px solid rgba(226, 232, 240, .85); }
.assistant-title { display: flex; align-items: center; gap: 10px; }
.assistant-avatar { width: 40px; height: 40px; border-radius: 14px; object-fit: cover; }
.assistant-title strong, .assistant-title span { display: block; white-space: nowrap; }
.assistant-title span { margin-top: 3px; color: #64748b; font-size: 12px; }
.assistant-actions { display: flex; align-items: center; gap: 6px; }
.assistant-enter, .assistant-close { border: 0; font: inherit; cursor: pointer; }
.assistant-enter { height: 32px; padding: 0 10px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 900; white-space: nowrap; }
.assistant-close { width: 32px; height: 32px; border-radius: 50%; background: #f1f5f9; color: #475569; font-size: 20px; line-height: 1; }
.assistant-body { flex: 1; padding: 14px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; background: rgba(248, 250, 252, .72); }
.assistant-message { max-width: 84%; padding: 10px 12px; border-radius: 15px; font-size: 13px; line-height: 1.65; white-space: pre-wrap; word-break: break-word; }
.assistant-message.assistant { align-self: flex-start; color: #1e293b; background: #fff; border: 1px solid rgba(226, 232, 240, .9); border-bottom-left-radius: 6px; }
.assistant-message.user { align-self: flex-end; color: #fff; background: #2563eb; border-bottom-right-radius: 6px; }
.assistant-message.loading { display: inline-flex; align-items: center; gap: 5px; width: fit-content; }
.assistant-message.loading span { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; animation: dotPulse 1s ease-in-out infinite; }
.assistant-message.loading span:nth-child(2) { animation-delay: .12s; }
.assistant-message.loading span:nth-child(3) { animation-delay: .24s; }
.assistant-order-card { margin: 0 12px 10px; padding: 12px; display: grid; gap: 8px; border: 1px solid #bfdbfe; border-radius: 14px; background: #eff6ff; color: #0f172a; }
.assistant-order-card strong { font-size: 13px; }
.assistant-order-card span { color: #475569; font-size: 12px; }
.assistant-order-card button { height: 34px; border: 0; border-radius: 10px; background: #2563eb; color: #fff; font: inherit; font-size: 12px; font-weight: 900; cursor: pointer; }
.assistant-order-card em { color: #16a34a; font-size: 12px; font-style: normal; font-weight: 900; }
.assistant-footer { padding: 12px; display: flex; gap: 8px; border-top: 1px solid rgba(226, 232, 240, .85); background: rgba(255, 255, 255, .9); }
.assistant-footer input { min-width: 0; flex: 1; height: 42px; padding: 0 12px; border-radius: 14px; border: 1px solid #cbd5e1; outline: none; background: #fff; color: #0f172a; font: inherit; font-size: 13px; }
.assistant-footer button { width: 58px; height: 42px; border: 0; border-radius: 14px; color: #fff; background: #2563eb; font: inherit; font-size: 13px; font-weight: 900; cursor: pointer; }
.assistant-footer button:disabled { cursor: not-allowed; opacity: .55; }
@keyframes dotPulse { 0%, 80%, 100% { transform: translateY(0); opacity: .45; } 40% { transform: translateY(-3px); opacity: 1; } }
@media (max-width: 1120px) { .hero-chain-search { width: min(100% - 42px, 1180px); padding: 20px; } .hero-chain-form { grid-template-columns: 1fr; } .demand-field { grid-column: auto; } .hero-plan-btn, .field-control { height: 56px; } .field-control input { height: 52px; } .field-label { font-size: 16px; } .home-trip-workspace, .landing-map-layout, .landing-map-visual-grid, .landing-map-detail-grid { grid-template-columns: 1fr; } .itinerary-module-grid { grid-template-columns: 1fr; } .route-option-grid, .hotel-match-strip, .service-timeline-grid { grid-template-columns: 1fr; } .landing-map-page { width: min(100% - 42px, 1180px); } .chain-stage-grid, .route-options, .hotel-match-grid, .chain-order-panel { grid-template-columns: 1fr; } .chain-stage-card.stage-order { grid-column: auto; } .intent-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .search-form, .rental-form, .private-form, .hotel-form { grid-template-columns: 1fr 1fr; } .search-btn { grid-column: 1 / -1; } .hotel-card, .traffic-row { grid-template-columns: 1fr; } .hotel-card img { width: 100%; height: 180px; } .price-box { justify-items: start; } .hotel-detail-hero, .hotel-detail-content { grid-template-columns: 1fr; } .hotel-info-panel { position: static; } }
@media (max-width: 1120px) { .search-form, .rental-form, .private-form, .hotel-form { grid-template-columns: 1fr 1fr; } .search-btn { grid-column: 1 / -1; } .hotel-card, .traffic-row { grid-template-columns: 1fr; } .hotel-card img { width: 100%; height: 180px; } .price-box { justify-items: start; } .hotel-detail-hero, .hotel-detail-content { grid-template-columns: 1fr; } .hotel-info-panel { position: static; } .assistant-panel { width: min(520px, calc(100vw - 32px)); min-width: 0; } .assistant-header { grid-template-columns: 1fr auto; } }
@media (max-width: 820px) { .home-hero { min-height: 650px; } .home-nav, .hero-chain-search, .hero-search-card, .booking-section, .chain-progress-section, .home-trip-workspace, .landing-map-page { width: min(100% - 28px, 1180px); } .landing-map-header { display: grid; padding: 20px; } .landing-map-card { min-height: 460px; } .landing-map-container { height: 460px; } .hero-chain-search { padding: 18px 16px; border-radius: 20px; } .field-control { grid-template-columns: 32px minmax(0, 1fr) auto auto; gap: 8px; padding: 0 12px; } .demand-field .field-control { grid-template-columns: 32px minmax(0, 1fr) auto; } .field-main-icon { width: 30px; height: 30px; font-size: 15px; } .field-control input { font-size: 15px; } .itinerary-card-grid { grid-template-columns: 1fr; } .trip-panel-head { display: grid; } .intent-grid { grid-template-columns: 1fr; } .clarify-card > div { grid-template-columns: 1fr; } .chain-order-actions { justify-content: stretch; } .chain-order-actions button { flex: 1 1 140px; } .travel-tabs { justify-content: flex-start; gap: 18px; padding-inline: 18px; } .search-form { grid-template-columns: 1fr; } .hotel-detail-hero, .hotel-detail-content { width: min(100% - 28px, 1180px); } .hotel-detail-cover, .hotel-detail-summary { min-height: auto; } .hotel-detail-summary { padding: 22px; } .detail-room-row { grid-template-columns: 1fr; } .room-price { text-align: left; } .assistant-fab { right: 16px; bottom: 16px; } .assistant-panel { right: 16px; bottom: 84px; width: calc(100vw - 32px); height: min(560px, calc(100vh - 160px)); } }
@media (max-width: 1120px) {
  .hero-chain-search { width: min(94%, 1180px); padding: 28px; border-radius: 28px; }
  .hero-location-row { grid-template-columns: 1fr; gap: 18px; }
  .hero-chain-form { gap: 18px; }
  .field-control { height: 68px; grid-template-columns: 42px minmax(0, 1fr) auto auto; }
  .demand-control { height: auto; min-height: 68px; grid-template-columns: 42px minmax(0, 1fr) auto 280px; }
  .field-control input { height: 64px; font-size: 18px; }
  .hero-plan-btn { min-width: 240px; height: 54px; }
}
@media (max-width: 820px) {
  .hero-chain-search { width: min(100% - 28px, 1180px); padding: 20px; border-radius: 24px; }
  .field-label { font-size: 16px; }
  .field-control { height: 62px; grid-template-columns: 34px minmax(0, 1fr) auto auto; gap: 8px; padding: 0 12px; border-radius: 16px; }
  .field-main-icon { width: 32px; height: 32px; font-size: 15px; }
  .field-control input { height: 58px; font-size: 16px; }
  .demand-control { height: auto; grid-template-columns: 34px minmax(0, 1fr) auto; padding: 0 12px 12px; }
  .demand-control .hero-plan-btn { grid-column: 1 / -1; min-width: 0; width: 100%; height: 54px; margin-top: 0; }
}
</style>
