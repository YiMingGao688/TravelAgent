<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  detail: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  actionLoading: { type: Boolean, default: false }
})

const emit = defineEmits(['supplement-plan', 'confirm-candidate', 'reject-candidate', 'change-candidate', 'finalize-plan'])

const run = computed(() => props.detail?.run || null)
const agentEvents = computed(() => props.detail?.events || [])
const tripCards = computed(() => props.detail?.cards || [])
const finalItinerary = computed(() => props.detail?.finalItinerary || [])
const session = computed(() => {
  if (run.value) {
    return {
      id: run.value.id,
      sessionTitle: `${run.value.startPlace || '起点待确认'} 至 ${run.value.endPlace || '终点待确认'}`,
      status: run.value.status,
      currentStep: run.value.currentStage
    }
  }
  return props.detail?.session || null
})
const steps = computed(() => {
  if (agentEvents.value.length) {
    return agentEvents.value.map(event => ({
      id: event.id || `${event.stage}-${event.sequenceNo}`,
      agentName: 'AgentWorkflow',
      stepKey: event.stage,
      stepTitle: event.title,
      stepContent: event.summary,
      status: event.status,
      sequenceNo: event.sequenceNo
    }))
  }
  return props.detail?.steps || []
})
const items = computed(() => {
  if (run.value) {
    const autoCards = tripCards.value
      .filter(card => card.confirmRequired === false)
      .map(card => ({
        id: `auto-${card.id}`,
        itemType: card.cardType,
        title: card.title,
        startPlace: card.subtitle,
        endPlace: '',
        startTime: '',
        endTime: '',
        price: null
      }))
    const confirmedItems = finalItinerary.value.map(item => ({
      id: item.id,
      itemType: 'CONFIRMED',
      title: item.title,
      startPlace: item.location,
      endPlace: '',
      startTime: item.startTime,
      endTime: item.endTime,
      price: null
    }))
    return [...autoCards, ...confirmedItems]
  }
  return props.detail?.items || []
})
const candidates = computed(() => {
  if (run.value) {
    return tripCards.value
      .filter(card => card.confirmRequired !== false)
      .map(card => ({
        id: card.id,
        candidateType: card.cardType,
        title: card.title,
        provider: card.subtitle,
        reason: card.content,
        price: null,
        status: card.confirmed ? 'SELECTED' : card.status || 'PENDING'
      }))
  }
  return props.detail?.candidates || []
})
const pendingCandidates = computed(() => candidates.value.filter(item => item.status === 'PENDING'))
const historyCandidates = computed(() => candidates.value.filter(item => item.status !== 'PENDING'))
const summary = computed(() => props.detail?.summary || null)
const readyToFinalize = computed(() => {
  if (run.value) return pendingCandidates.value.length === 0 && candidates.value.length > 0
  return Boolean(props.detail?.readyToFinalize)
})

const supplementForm = ref({
  departTime: '',
  returnTime: '',
  budget: '',
  preference: ''
})

const loadingSteps = [
  { id: 'loading-intent', agentName: 'IntentAgent', stepKey: 'INTENT_PARSE', stepTitle: '解析用户需求', stepContent: '正在识别出发地、目的地、时间偏好、预算和服务诉求。', status: 'RUNNING' },
  { id: 'loading-route', agentName: 'RouteAgent', stepKey: 'ROUTE_PLAN', stepTitle: '生成路线候选', stepContent: '正在结合距离、换乘、步行量和交通方式生成候选方案。', status: 'WAITING' },
  { id: 'loading-resource', agentName: 'TicketAgent', stepKey: 'RESOURCE_MATCH', stepTitle: '匹配票务与住宿', stepContent: '正在准备高铁、机票、酒店、打车和提醒事项候选项。', status: 'WAITING' },
  { id: 'loading-risk', agentName: 'RiskAgent', stepKey: 'RISK_CHECK', stepTitle: '评估风险提醒', stepContent: '正在检查天气、时间余量、路况和证件提醒。', status: 'WAITING' }
]

const visibleSteps = computed(() => {
  if (steps.value.length) return steps.value
  return []
})

const missingFields = computed(() => {
  const result = []
  if (session.value && !session.value.departTime) result.push('出发日期和时间')
  if (session.value && (session.value.budget === null || session.value.budget === undefined || session.value.budget === '')) result.push('预算')
  return result
})

const hasMissingFields = computed(() => Boolean(session.value && missingFields.value.length))

function submitSupplement() {
  const payload = {}
  if (supplementForm.value.departTime) payload.departTime = supplementForm.value.departTime
  if (supplementForm.value.returnTime) payload.returnTime = supplementForm.value.returnTime
  if (supplementForm.value.budget !== '') payload.budget = Number(supplementForm.value.budget)
  if (supplementForm.value.preference.trim()) payload.preference = supplementForm.value.preference.trim()
  if (!Object.keys(payload).length || props.actionLoading) return
  emit('supplement-plan', payload)
  supplementForm.value.preference = ''
}

function money(value) {
  if (value === null || value === undefined || value === '') return '待确认'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return '待确认'
  return `￥${numberValue.toFixed(0)}`
}

function timeRange(item) {
  const start = item.startTime ? String(item.startTime).replace('T', ' ').slice(5, 16) : ''
  const end = item.endTime ? String(item.endTime).replace('T', ' ').slice(5, 16) : ''
  return [start, end].filter(Boolean).join(' 至 ') || '时间待确认'
}

function typeLabel(type) {
  const map = {
    TRAIN: '高铁',
    FLIGHT: '机票',
    HOTEL: '酒店',
    TAXI: '打车',
    METRO: '地铁',
    BUS: '公交',
    SCENIC: '景点',
    REMINDER: '提醒',
    PREPARE: '准备',
    TRANSPORT: '交通',
    TRANSFER: '接驳',
    CONFIRMED: '已确认'
  }
  return map[type] || '行程'
}

function statusLabel(status) {
  const map = {
    PENDING: '待确认',
    SELECTED: '已加入',
    REJECTED: '已忽略',
    EXPIRED: '已换方案',
    RUNNING: '分析中',
    SUCCESS: '已完成',
    FAILED: '失败',
    WAITING: '排队中',
    WAITING_CONFIRM: '待确认',
    SUMMARY_READY: '已生成总行程',
    DONE: '已完成',
    AUTO_INCLUDED: '自动加入',
    CONFIRMED: '已加入'
  }
  return map[status] || '处理中'
}

function agentLabel(name) {
  const map = {
    TravelMasterAgent: '总控智能体',
    IntentAgent: '需求解析智能体',
    RouteAgent: '路线智能体',
    TicketAgent: '票务智能体',
    HotelAgent: '酒店智能体',
    RiskAgent: '风险智能体',
    ReminderAgent: '提醒智能体',
    ReportAgent: '报告智能体',
    AgentWorkflow: '工作流事件'
  }
  return map[name] || '智能体'
}

function stepTitle(step) {
  const map = {
    MASTER_START: '启动规划会话',
    INTENT_PARSE: '解析用户需求',
    ROUTE_PLAN: '生成路线候选',
    TICKET_PLAN: '生成票务候选',
    HOTEL_PLAN: '生成酒店候选',
    RISK_CHECK: '评估风险提醒',
    REMINDER_PLAN: '生成提醒事项',
    REPORT_SUMMARY: '生成总行程卡片',
    USER_SUPPLEMENT: '补充缺失信息',
    RESOURCE_MATCH: '匹配票务与住宿',
    REQUIREMENT_ANALYSIS: '解析用户需求',
    ROUTE_CANDIDATE_GENERATION: '生成路线候选',
    TICKET_HOTEL_MATCHING: '匹配票务和住宿候选',
    RISK_EVALUATION: '评估风险',
    CARD_GENERATION: '生成行程卡片',
    USER_CONFIRMATION_WAITING: '等待用户确认',
    FINAL_ITINERARY_GENERATION: '生成最终行程单'
  }
  return map[step?.stepKey] || step?.stepTitle || '处理规划步骤'
}

function stepContent(step) {
  const content = step?.stepContent || ''
  if (!content) return '正在处理当前规划步骤。'
  return /[A-Za-z]/.test(content)
    ? `${stepTitle(step)}已写入工作流记录，当前会话继续推进。`
    : content
}

function cleanText(value, fallback) {
  const text = String(value || '').trim()
  if (!text || /[A-Za-z]/.test(text)) return fallback
  return text
}

function providerText(value) {
  return cleanText(value, '服务候选')
}

function riskText(value) {
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return map[value] || cleanText(value, '待评估')
}
</script>

<template>
  <section class="agent-plan-workspace" aria-label="真实智能体行程工作流">
    <header class="agent-plan-head">
      <div>
        <span>智能体规划</span>
        <strong>{{ cleanText(session?.sessionTitle, '正在生成行程方案') }}</strong>
      </div>
      <em>{{ loading ? '思考中' : statusLabel(session?.status) }}</em>
    </header>

    <div class="agent-plan-grid">
      <div class="trip-card-stream">
        <div class="stream-title">
          <strong>行程卡片流</strong>
          <span>{{ items.length }} 项已加入 / {{ pendingCandidates.length }} 项待确认</span>
        </div>

        <div v-if="loading && !items.length && !pendingCandidates.length" class="thinking-card-list">
          <article v-for="index in 3" :key="index" class="thinking-card" :style="{ '--delay': `${index * 120}ms` }">
            <i></i>
            <div>
              <strong></strong>
              <span></span>
              <span></span>
            </div>
          </article>
        </div>

        <form v-if="hasMissingFields" class="supplement-popover" @submit.prevent="submitSupplement">
          <header>
            <div>
              <strong>还需要补充：{{ missingFields.join('、') }}</strong>
              <span>补充后续接当前会话，不会重新规划整条路线。</span>
            </div>
          </header>
          <div class="supplement-grid">
            <label v-if="missingFields.includes('出发日期和时间')">
              <span>出发日期和时间</span>
              <input v-model="supplementForm.departTime" type="datetime-local" />
            </label>
            <label>
              <span>返程时间</span>
              <input v-model="supplementForm.returnTime" type="datetime-local" />
            </label>
            <label v-if="missingFields.includes('预算')">
              <span>预算</span>
              <input v-model="supplementForm.budget" type="number" min="0" step="50" placeholder="例如 1500" />
            </label>
            <label>
              <span>补充偏好</span>
              <input v-model="supplementForm.preference" placeholder="例如 靠窗、少换乘、不要太早出发" />
            </label>
          </div>
          <button type="submit" :disabled="actionLoading">保存并继续当前规划</button>
        </form>

        <article v-for="item in items" :key="`item-${item.id}`" class="trip-flow-card confirmed">
          <div class="card-type">{{ typeLabel(item.itemType) }}</div>
          <div>
            <strong>{{ cleanText(item.title, `${typeLabel(item.itemType)}行程`) }}</strong>
            <p>{{ item.startPlace || '起点待确认' }} 至 {{ item.endPlace || '终点待确认' }}</p>
            <small>{{ timeRange(item) }}</small>
          </div>
          <b>{{ money(item.price) }}</b>
        </article>

        <article v-for="candidate in pendingCandidates" :key="`candidate-${candidate.id}`" class="trip-flow-card candidate">
          <div class="card-type">{{ typeLabel(candidate.candidateType) }}</div>
          <div>
            <strong>{{ cleanText(candidate.title, `${typeLabel(candidate.candidateType)}候选方案`) }}</strong>
            <p>{{ cleanText(candidate.reason, '智能体生成候选项，等待用户确认后加入行程单。') }}</p>
            <small>{{ providerText(candidate.provider) }} · {{ money(candidate.price) }}</small>
          </div>
          <div class="candidate-actions">
            <button type="button" :disabled="actionLoading" @click="emit('confirm-candidate', candidate)">确认加入行程</button>
            <button type="button" :disabled="actionLoading" @click="emit('change-candidate', candidate)">换一个方案</button>
            <button type="button" :disabled="actionLoading" @click="emit('reject-candidate', candidate)">暂不添加</button>
          </div>
        </article>

        <div v-if="!loading && !items.length && !pendingCandidates.length" class="agent-empty">
          暂无行程卡片，请先在上方填写信息并开始规划。
        </div>

        <details v-if="historyCandidates.length" class="candidate-history">
          <summary>已处理候选项 {{ historyCandidates.length }} 项</summary>
          <span v-for="candidate in historyCandidates" :key="`history-${candidate.id}`">
            {{ typeLabel(candidate.candidateType) }} · {{ cleanText(candidate.title, '候选方案') }} · {{ statusLabel(candidate.status) }}
          </span>
        </details>
      </div>

      <aside class="agent-workflow-panel">
        <div class="stream-title">
          <strong>思考过程</strong>
          <span>{{ visibleSteps.length }} 个步骤</span>
        </div>
        <article v-for="(step, index) in visibleSteps" :key="step.id" class="agent-step" :class="String(step.status || '').toLowerCase()" :style="{ '--delay': `${index * 90}ms` }">
          <i></i>
          <div>
            <span>{{ agentLabel(step.agentName) }} · {{ statusLabel(step.status) }}</span>
            <strong>{{ stepTitle(step) }}</strong>
            <p>{{ stepContent(step) }}</p>
          </div>
        </article>
        <div v-if="!visibleSteps.length" class="agent-empty">等待开始规划。</div>
      </aside>
    </div>

    <footer v-if="summary || readyToFinalize" class="trip-summary-card">
      <div>
        <span>总行程卡片</span>
        <strong>{{ cleanText(summary?.title, '可生成总行程卡片') }}</strong>
        <p>{{ cleanText(summary?.summaryText, '所有待确认项处理后，可生成总耗时、总费用、风险等级和提醒事项。') }}</p>
        <small>总费用 {{ money(summary?.totalPrice) }} · 风险 {{ riskText(summary?.riskLevel) }} · {{ cleanText(summary?.totalDuration, '总耗时待生成') }}</small>
      </div>
      <div class="summary-actions">
        <button type="button" :disabled="actionLoading || !readyToFinalize" @click="emit('finalize-plan')">确认最终行程</button>
        <button type="button" :disabled="!summary">导出报告</button>
        <button type="button" :disabled="!summary">保存方案</button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.agent-plan-workspace {
  width: 100%;
  margin: 0;
  padding: 20px;
  border: 1px solid rgba(47, 94, 153, 0.14);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.98)),
    #fff;
  box-shadow: 0 20px 50px rgba(20, 57, 99, 0.12);
  color: #15304f;
  animation: workspaceEnter .34s ease both;
}

.agent-plan-head,
.stream-title,
.trip-summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.agent-plan-head {
  min-height: 58px;
  padding: 2px 2px 14px;
  border-bottom: 1px solid #e5edf7;
}

.agent-plan-head span,
.stream-title span,
.trip-summary-card span {
  display: block;
  color: #5d7595;
  font-size: 12px;
}

.agent-plan-head strong,
.stream-title strong,
.trip-summary-card strong {
  display: block;
  color: #102843;
  font-size: 18px;
}

.agent-plan-head em {
  min-width: 82px;
  padding: 7px 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #1f5fba;
  font-style: normal;
  font-weight: 800;
  text-align: center;
}

.agent-plan-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(330px, 0.9fr);
  gap: 18px;
  margin-top: 18px;
}

.trip-card-stream,
.agent-workflow-panel {
  min-width: 0;
}

.trip-flow-card,
.agent-step,
.supplement-popover,
.trip-summary-card,
.agent-empty,
.candidate-history {
  animation: cardIn .34s ease both;
}

.trip-flow-card {
  position: relative;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #d9e5f5;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(25, 71, 126, 0.06);
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
}

.trip-flow-card:hover {
  transform: translateY(-2px);
  border-color: #a9c9ef;
  box-shadow: 0 16px 32px rgba(25, 71, 126, 0.1);
}

.trip-flow-card.confirmed::before,
.trip-flow-card.candidate::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 999px;
  background: #2f9d65;
}

.trip-flow-card.candidate {
  grid-template-columns: 74px minmax(0, 1fr);
  border-color: #b9d2f2;
  background: #f8fbff;
}

.trip-flow-card.candidate::before {
  background: #1f6fd1;
}

.card-type {
  display: grid;
  place-items: center;
  min-height: 48px;
  border-radius: 10px;
  background: #e8f1ff;
  color: #1d5da8;
  font-weight: 900;
  font-size: 13px;
}

.trip-flow-card p,
.agent-step p,
.trip-summary-card p {
  margin: 5px 0;
  color: #4d6480;
  line-height: 1.5;
}

.trip-flow-card small,
.trip-summary-card small {
  color: #6d8199;
}

.candidate-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.candidate-actions button,
.summary-actions button {
  border: 1px solid #cbd9ea;
  border-radius: 8px;
  background: #fff;
  color: #244766;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
  transition: transform .16s ease, box-shadow .16s ease, background-color .16s ease;
}

.candidate-actions button:hover,
.summary-actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(31, 111, 209, 0.12);
}

.candidate-actions button:first-child,
.summary-actions button:first-child {
  border-color: #1f6fd1;
  background: #1f6fd1;
  color: #fff;
}

.candidate-actions button:disabled,
.summary-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
  box-shadow: none;
}

.agent-step {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 10px;
  margin-top: 12px;
  padding: 13px;
  border: 1px solid #e0e8f2;
  border-radius: 10px;
  background: #fff;
  animation-delay: var(--delay);
}

.agent-step i {
  position: relative;
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: #94a8c0;
}

.agent-step i::after {
  content: "";
  position: absolute;
  inset: -5px;
  border-radius: inherit;
  border: 1px solid rgba(31, 111, 209, 0.22);
  opacity: 0;
}

.agent-step.running i {
  background: #1f6fd1;
  animation: pointPulse 1.3s ease infinite;
}

.agent-step.running i::after {
  animation: ringPulse 1.3s ease infinite;
}

.agent-step.success i {
  background: #2f9d65;
}

.agent-step.failed i {
  background: #d9534f;
}

.agent-step span {
  color: #6e829a;
  font-size: 12px;
}

.thinking-card-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.thinking-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  border: 1px solid #d9e5f5;
  border-radius: 10px;
  background: #fff;
  animation: cardIn .34s ease both;
  animation-delay: var(--delay);
}

.thinking-card i,
.thinking-card strong,
.thinking-card span {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, #edf4ff 0%, #f8fbff 45%, #edf4ff 100%);
  background-size: 220% 100%;
  animation: shimmer 1.3s ease-in-out infinite;
}

.thinking-card i {
  height: 48px;
}

.thinking-card strong {
  width: 52%;
  height: 16px;
  margin-top: 4px;
}

.thinking-card span {
  width: 86%;
  height: 12px;
  margin-top: 10px;
}

.thinking-card span:last-child {
  width: 64%;
}

.supplement-popover {
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #b9d2f2;
  border-radius: 10px;
  background: #f8fbff;
  box-shadow: 0 10px 24px rgba(31, 111, 209, 0.1);
}

.supplement-popover header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.supplement-popover strong,
.supplement-popover span {
  display: block;
}

.supplement-popover span,
.supplement-grid label span {
  color: #5d7595;
  font-size: 12px;
}

.supplement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.supplement-grid label {
  display: grid;
  gap: 6px;
}

.supplement-grid input {
  min-width: 0;
  height: 40px;
  border: 1px solid #cbd9ea;
  border-radius: 8px;
  padding: 0 10px;
  color: #15304f;
  font: inherit;
  background: #fff;
}

.supplement-popover > button {
  margin-top: 12px;
  height: 38px;
  border: 0;
  border-radius: 8px;
  padding: 0 14px;
  background: #1f6fd1;
  color: #fff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.supplement-popover > button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.agent-empty,
.candidate-history {
  margin-top: 12px;
  padding: 14px;
  border-radius: 10px;
  background: #f4f7fb;
  color: #60758d;
}

.candidate-history span {
  display: block;
  margin-top: 8px;
}

.trip-summary-card {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #b9d2f2;
  border-radius: 10px;
  background: #f8fbff;
}

.summary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@keyframes workspaceEnter {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pointPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.22); }
}

@keyframes ringPulse {
  0% { opacity: .7; transform: scale(.65); }
  100% { opacity: 0; transform: scale(1.75); }
}

@keyframes shimmer {
  0% { background-position: 120% 0; }
  100% { background-position: -120% 0; }
}

@media (max-width: 900px) {
  .agent-plan-workspace {
    width: min(100% - 28px, 680px);
  }

  .agent-plan-grid,
  .trip-flow-card,
  .trip-flow-card.candidate,
  .trip-summary-card,
  .supplement-grid {
    grid-template-columns: 1fr;
  }

  .agent-plan-head,
  .stream-title,
  .trip-summary-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
