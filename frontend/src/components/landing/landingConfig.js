import logoUrl from '../../assets/logo.png'
import heroBgUrl from '../../assets/images/hero-bg.png'
import hotelResultBgUrl from '../../assets/images/hotel-result-bg.png'
import driveResultBgUrl from '../../assets/images/result-drive-bg.png'
import busResultBgUrl from '../../assets/images/result-bus-bg.png'
import subwayResultBgUrl from '../../assets/images/result-subway-bg.png'
import trainResultBgUrl from '../../assets/images/result-train-bg.png'
import flightResultBgUrl from '../../assets/images/result-flight-bg.png'
import cruiseResultBgUrl from '../../assets/images/result-cruise-bg.png'
import stayIcon from '../../assets/icons/stay.png'
import driveIcon from '../../assets/icons/drive.png'
import metroIcon from '../../assets/icons/metro.png'
import railIcon from '../../assets/icons/rail.png'
import flightIcon from '../../assets/icons/flight.png'
import cruiseIcon from '../../assets/icons/cruise.png'
import busIcon from '../../assets/icons/bus.png'
import xiaoqiAvatar from '../../assets/icons/xiaoqi-avatar.png'

export {
  logoUrl,
  heroBgUrl,
  hotelResultBgUrl,
  driveResultBgUrl,
  busResultBgUrl,
  subwayResultBgUrl,
  trainResultBgUrl,
  flightResultBgUrl,
  cruiseResultBgUrl,
  xiaoqiAvatar
}

export const hotDemandChips = [
  '从北京南站去八达岭长城',
  '从上海虹桥站去迪士尼乐园',
  '从杭州西湖去灵隐寺',
  '从广州南站去长隆度假区',
  '从成都东站去都江堰景区'
]

export const planningStageMeta = [
  { key: 'intent', title: 'AI 已识别需求', thinking: 'AI 正在识别需求...' },
  { key: 'route', title: '推荐出行方案', thinking: 'AI 正在生成推荐方案...' },
  { key: 'traffic', title: '智能交通产品匹配', thinking: 'AI 正在匹配交通产品...' },
  { key: 'hotel', title: '智能住宿匹配', thinking: 'AI 正在匹配住宿资源...' },
  { key: 'risk', title: '动态风险预警', thinking: 'AI 正在检查天气与路线风险...' },
  { key: 'timeline', title: '全过程服务时间轴', thinking: 'AI 正在生成全过程服务时间轴...' },
  { key: 'order', title: '模拟预约 / 一键订购', thinking: 'AI 正在生成模拟预约...' }
]

export const planningStageOrder = ['intent', 'route', 'traffic', 'hotel', 'risk', 'timeline', 'order']

export const enumLabelMap = {
  CAR: '自驾',
  car: '自驾',
  TRAIN: '高铁',
  train: '高铁',
  BUS: '公交',
  bus: '公交',
  SUBWAY: '地铁',
  METRO: '地铁',
  subway: '地铁',
  MIN_WALK: '少步行',
  min_walk: '少步行',
  SMART: '智能推荐',
  FASTEST: '最快优先',
  CHEAPEST: '低价优先',
  LESS_TRANSFER: '少换乘',
  SHORT_TRIP: '短途游',
  BUSINESS_TRIP: '商务出差',
  BUSINESS: '商务出差',
  TRAVEL: '旅游出行',
  GENERAL: '普通旅客',
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
}

export const travelTabs = [
  { id: 'hotel', name: '酒店', icon: stayIcon },
  { id: 'selfDrive', name: '自驾', icon: driveIcon },
  { id: 'bus', name: '公交', icon: busIcon },
  { id: 'subway', name: '地铁', icon: metroIcon },
  { id: 'train', name: '高铁', icon: railIcon },
  { id: 'flight', name: '飞机', icon: flightIcon },
  { id: 'cruise', name: '邮轮', icon: cruiseIcon }
]

export const sidebarEntries = [
  { id: 'hotel', name: '酒店', icon: 'fa-hotel', tabId: 'hotel' },
  { id: 'drive', name: '自驾', icon: 'fa-car', tabId: 'selfDrive' },
  { id: 'bus', name: '公交', icon: 'fa-bus', tabId: 'bus' },
  { id: 'subway', name: '地铁', icon: 'fa-train-subway', tabId: 'subway' },
  { id: 'train', name: '高铁', icon: 'fa-train', tabId: 'train' },
  { id: 'flight', name: '飞机', icon: 'fa-plane', tabId: 'flight' },
  { id: 'cruise', name: '邮轮', icon: 'fa-ship', tabId: 'cruise' }
]

sidebarEntries.push(
  { id: 'map-divider', divider: true },
  { id: 'map', name: '地图', icon: 'fa-map-location-dot', action: 'map' },
  { id: 'orders-divider', divider: true },
  { id: 'orders', name: '历史订单', icon: 'fa-receipt', action: 'orders' }
)

export const orderCategories = [
  { id: 'all', name: '全部历史订单' },
  { id: 'hotel', name: '酒店订单' },
  { id: 'train', name: '高铁订单' },
  { id: 'flight', name: '飞机订单' },
  { id: 'cruise', name: '邮轮订单' }
]
