<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const emit = defineEmits(['start'])
const scrolled = ref(false)
let observer = null

const capabilities = [
    'AI 对话收集需求',
    '结构化信息确认',
    'RAG 知识增强',
    '高德地图可视化',
    '动态风险预警',
    '行李智能服务',
    '智能售后工单',
    '出行服务报告'
]

const workflow = ['对话收集需求', '主动追问', '结构化确认', '工具调用', '方案生成', '闭环复用']

const scenes = [
    { title: '通勤出行', desc: '跨城通勤、日常换乘、时间优先路线。' },
    { title: '商务出行', desc: '会议差旅、接驳安排、票据与售后协助。' },
    { title: '旅游出行', desc: '多日行程、预算偏好、路线和服务报告。' },
    { title: '特殊群体出行', desc: '老人、亲子、少走路和行李协助。' }
]

function startUse() {
    emit('start')
}

function scrollToCapabilities() {
    document.querySelector('#capabilities')?.scrollIntoView({ behavior: 'smooth' })
}

function handleScroll(event) {
    scrolled.value = event.target.scrollTop > 12
}

onMounted(() => {
    observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible')
                observer.unobserve(entry.target)
            }
        })
    }, { threshold: 0.14 })

    document.querySelectorAll('.reveal').forEach((el, index) => {
        el.style.setProperty('--delay', `${Math.min(index % 8, 7) * 70}ms`)
        observer.observe(el)
    })
})

onBeforeUnmount(() => {
    observer?.disconnect()
})
</script>

<template>
    <main class="landing-page" @scroll="handleScroll">
        <header :class="['landing-header', { scrolled }]">
            <div class="brand">
                <div class="brand-mark"><i class="fa-solid fa-plane-departure"></i></div>
                <div>
                    <strong>出行助手</strong>
                    <span>智慧出行智能体平台</span>
                </div>
            </div>
            <nav>
                <a href="#intro">平台介绍</a>
                <a href="#capabilities">核心能力</a>
                <a href="#workflow">工作流程</a>
                <a href="#scenes">适用场景</a>
                <button type="button" @click="startUse">开始使用</button>
            </nav>
        </header>

        <section id="intro" class="hero">
            <div class="hero-copy reveal">
                <p class="eyebrow">智慧出行智能体平台</p>
                <h1>
                    <span>你的个人</span>
                    <strong>智慧出行智能体</strong>
                </h1>
                <p class="subtitle">
                    通过 AI 对话收集出行需求，主动追问缺失条件，结构化识别起点、终点、预算、偏好和场景，联动知识库、地图、天气、风险预警、行李服务和售后工单，生成可视化、可保存、可复用的出行服务方案。
                </p>
                <div class="hero-actions">
                    <button class="primary" type="button" @click="startUse">开始使用</button>
                    <button class="secondary" type="button" @click="scrollToCapabilities">了解平台能力</button>
                </div>
            </div>

        </section>

        <section class="section intro-section reveal">
            <div class="section-title">
                <span>平台介绍</span>
                <h2>不是普通表单，也不是普通聊天机器人</h2>
            </div>
            <p>
                出行助手把旅客的一句话需求转化为可执行的出行服务流程：对话收集需求 → 主动追问 → 结构化确认 → 工具调用 → 方案生成 → 售后复用。
            </p>
        </section>

        <section id="capabilities" class="section">
            <div class="section-title reveal">
                <span>核心能力</span>
                <h2>从一句话需求到完整出行服务</h2>
            </div>
            <div class="capability-grid">
                <article v-for="item in capabilities" :key="item" class="reveal">
                    <i class="fa-regular fa-circle-check"></i>
                    <strong>{{ item }}</strong>
                </article>
            </div>
        </section>

        <section id="workflow" class="section workflow">
            <div class="section-title reveal">
                <span>工作流程</span>
                <h2>让需求、路线、服务和报告自然衔接</h2>
            </div>
            <div class="steps">
                <div v-for="step in workflow" :key="step" class="reveal">{{ step }}</div>
            </div>
        </section>

        <section id="scenes" class="section scenes">
            <div class="section-title reveal">
                <span>适用场景</span>
                <h2>面向真实旅客的出行服务</h2>
            </div>
            <div class="scene-grid">
                <article v-for="item in scenes" :key="item.title" class="reveal">
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.desc }}</p>
                </article>
            </div>
        </section>

        <section class="bottom-cta reveal">
            <h2>开始使用出行助手</h2>
            <p>进入系统后可使用 AI 咨询、路线规划、风险预警、时间轴、行李服务、售后工单和报告生成等功能。</p>
            <button class="primary" type="button" @click="startUse">开始使用</button>
        </section>
    </main>
</template>

<style scoped>
.landing-page {
    position: relative;
    height: 100vh;
    overflow-y: auto;
    background:
        linear-gradient(180deg, rgba(232, 241, 255, 0.78) 0%, rgba(247, 249, 252, 0.94) 34%, #f7f9fc 100%);
    color: #111827;
    scroll-behavior: smooth;
}

.landing-page::before {
    content: "";
    position: fixed;
    inset: 0;
    pointer-events: none;
    background-image:
        linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
        linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
    background-size: 42px 42px;
    mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.62), rgba(0, 0, 0, 0.12) 48%, transparent 78%);
}

.landing-page::after {
    content: "";
    position: fixed;
    top: 72px;
    left: 50%;
    width: min(960px, calc(100vw - 48px));
    height: 1px;
    transform: translateX(-50%);
    pointer-events: none;
    background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.28), transparent);
}

.landing-header {
    position: sticky;
    top: 0;
    z-index: 10;
    height: 72px;
    padding: 0 48px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: rgba(247, 249, 252, 0.78);
    border-bottom: 1px solid rgba(15, 23, 42, 0.06);
    backdrop-filter: blur(14px);
    transition: box-shadow 180ms ease, background 180ms ease;
}

.landing-header.scrolled {
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
}

.brand-mark {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    background: #2563eb;
    color: #fff;
}

.brand strong,
.brand span {
    display: block;
}

.brand strong {
    font-size: 17px;
}

.brand span {
    margin-top: 2px;
    color: #64748b;
    font-size: 12px;
}

nav {
    display: flex;
    align-items: center;
    gap: 22px;
}

nav a {
    color: #475569;
    text-decoration: none;
    font-size: 14px;
}

nav a:hover {
    color: #111827;
}

button {
    border: 0;
    cursor: pointer;
    font: inherit;
}

nav button,
.primary {
    padding: 11px 18px;
    border-radius: 999px;
    background: #2563eb;
    color: #fff;
    box-shadow: 0 10px 22px rgba(37, 99, 235, 0.16);
}

.secondary {
    padding: 11px 18px;
    border-radius: 999px;
    border: 1px solid #d8e3f7;
    background: #fff;
    color: #111827;
}

.hero {
    position: relative;
    max-width: 1040px;
    margin: 0 auto;
    padding: 124px 24px 100px;
    text-align: center;
}

.hero::before {
    content: "";
    position: absolute;
    inset: 44px 24px 34px;
    z-index: -1;
    border-radius: 36px;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.3));
    border: 1px solid rgba(255, 255, 255, 0.72);
    box-shadow: 0 30px 90px rgba(37, 99, 235, 0.08);
}

.hero-copy {
    text-align: center;
}

.eyebrow,
.section-title span {
    color: #2563eb;
    font-size: 14px;
    font-weight: 700;
}

.hero h1 {
    max-width: 900px;
    margin: 18px auto 22px;
    font-size: clamp(44px, 6vw, 76px);
    line-height: 1.02;
    letter-spacing: 0;
}

.hero h1 span,
.hero h1 strong {
    display: block;
}

.hero h1 span {
    color: #475569;
    font-size: clamp(28px, 3.4vw, 42px);
    font-weight: 650;
    line-height: 1.15;
}

.hero h1 strong {
    margin-top: 8px;
    color: #0f172a;
    font-weight: 850;
}

.hero h1::after {
    content: "";
    display: block;
    width: 82px;
    height: 4px;
    margin: 24px auto 0;
    border-radius: 999px;
    background: #2563eb;
}

.subtitle {
    max-width: 790px;
    margin: 0 auto;
    color: #475569;
    font-size: 18px;
    line-height: 1.9;
}

.hero-actions {
    margin-top: 34px;
    display: flex;
    justify-content: center;
    gap: 12px;
    flex-wrap: wrap;
}

.section {
    position: relative;
    max-width: 1120px;
    margin: 0 auto;
    padding: 68px 24px;
}

.intro-section {
    padding-top: 28px;
}

.intro-section p {
    max-width: 850px;
    color: #475569;
    font-size: 18px;
    line-height: 1.9;
}

.section-title {
    margin-bottom: 24px;
}

.section-title h2 {
    margin-top: 10px;
    font-size: 32px;
    line-height: 1.25;
}

.capability-grid,
.scene-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.capability-grid article,
.scene-grid article,
.steps div {
    background: #fff;
    border: 1px solid rgba(15, 23, 42, 0.08);
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
    transition: transform 180ms ease, box-shadow 180ms ease;
}

.capability-grid article:hover,
.scene-grid article:hover,
.steps div:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 42px rgba(15, 23, 42, 0.09);
}

.capability-grid article,
.scene-grid article {
    min-height: 116px;
    padding: 22px;
    border-radius: 18px;
}

.capability-grid i {
    color: #2563eb;
    margin-bottom: 18px;
}

.capability-grid strong {
    display: block;
    font-size: 16px;
}

.workflow {
    border-top: 1px solid rgba(15, 23, 42, 0.07);
    border-bottom: 1px solid rgba(15, 23, 42, 0.07);
}

.steps {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 10px;
}

.steps div {
    min-height: 82px;
    padding: 16px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    color: #334155;
    font-weight: 700;
}

.scene-grid article h3 {
    margin: 0 0 10px;
    font-size: 18px;
}

.scene-grid article p {
    margin: 0;
    color: #64748b;
    line-height: 1.7;
}

.bottom-cta {
    position: relative;
    max-width: 920px;
    margin: 36px auto 88px;
    padding: 44px 28px;
    border-radius: 24px;
    background: #fff;
    border: 1px solid rgba(15, 23, 42, 0.08);
    text-align: center;
    box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
}

.bottom-cta h2 {
    margin: 0;
    font-size: 34px;
}

.bottom-cta p {
    max-width: 680px;
    margin: 14px auto 24px;
    color: #64748b;
    line-height: 1.8;
}

.reveal {
    opacity: 0;
    transform: translateY(28px);
    transition: opacity 520ms ease, transform 520ms ease;
    transition-delay: var(--delay, 0ms);
}

.reveal.is-visible {
    opacity: 1;
    transform: translateY(0);
}

@media (max-width: 980px) {
    .landing-header {
        padding: 0 20px;
    }

    nav a {
        display: none;
    }

    .hero {
        padding-top: 86px;
    }

    .capability-grid,
    .scene-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .steps {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 560px) {
    .brand span {
        display: none;
    }

    .capability-grid,
    .scene-grid,
    .steps {
        grid-template-columns: 1fr;
    }
}
</style>
