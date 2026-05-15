<script setup>
const emit = defineEmits(['start'])

const capabilities = [
    'AI 对话收集需求',
    '结构化信息确认',
    '知识增强问答',
    '高德地图可视化',
    '动态风险预警',
    '行李智能服务',
    '智能售后工单',
    '出行服务报告'
]

const scenes = [
    { title: '通勤出行', desc: '跨城通勤、日常换乘、时间优先路线。' },
    { title: '商务出行', desc: '会议差旅、接驳安排、票据与售后协助。' },
    { title: '旅游出行', desc: '多日行程、预算偏好、路线和服务报告。' },
    { title: '特殊群体出行', desc: '老人、亲子、少走路和行李协助。' }
]

function scrollToCapabilities() {
    document.querySelector('#capabilities')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
    <main class="landing-page">
        <header class="landing-header">
            <div class="brand">
                <div class="brand-mark"><i class="fa-solid fa-plane-departure"></i></div>
                <div>
                    <strong>智能出行助手</strong>
                    <span>智慧出行智能体</span>
                </div>
            </div>
            <nav>
                <a href="#intro">平台介绍</a>
                <a href="#capabilities">核心能力</a>
                <a href="#workflow">工作流程</a>
                <a href="#scenes">适用场景</a>
                <button @click="emit('start')">开始使用</button>
            </nav>
        </header>

        <section id="intro" class="hero">
            <p class="eyebrow">AI 驱动的智慧出行全过程服务平台</p>
            <h1>你的个人智慧出行智能体</h1>
            <p class="subtitle">
                通过自然语言对话收集出行需求，联动知识库、地图、天气、风险预警、行李服务和售后工单，
                生成可视化、可保存、可复用的出行服务方案。
            </p>
            <div class="hero-actions">
                <button class="primary" @click="emit('start')">开始使用</button>
                <button class="secondary" @click="scrollToCapabilities">了解平台能力</button>
            </div>
        </section>

        <section id="capabilities" class="section">
            <div class="section-title">
                <span>核心能力</span>
                <h2>从一句话需求到完整出行服务</h2>
            </div>
            <div class="capability-grid">
                <article v-for="item in capabilities" :key="item">
                    <i class="fa-regular fa-circle-check"></i>
                    <strong>{{ item }}</strong>
                </article>
            </div>
        </section>

        <section id="workflow" class="section workflow">
            <div class="section-title">
                <span>工作流程</span>
                <h2>让需求、路线、服务和报告自然衔接</h2>
            </div>
            <div class="steps">
                <div v-for="step in ['对话收集需求', '主动追问', '结构化确认', '工具调用', '方案生成', '售后复用']" :key="step">
                    {{ step }}
                </div>
            </div>
        </section>

        <section id="scenes" class="section scenes">
            <div class="section-title">
                <span>适用场景</span>
                <h2>面向真实旅客的出行服务</h2>
            </div>
            <div class="scene-grid">
                <article v-for="item in scenes" :key="item.title">
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.desc }}</p>
                </article>
            </div>
        </section>
    </main>
</template>

<style scoped>
.landing-page {
    height: 100vh;
    overflow-y: auto;
    background: #f6f9ff;
    color: #102033;
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
    background: rgba(246, 249, 255, 0.92);
    border-bottom: 1px solid rgba(37, 99, 235, 0.1);
    backdrop-filter: blur(14px);
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
    color: #102033;
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
}

.secondary {
    padding: 11px 18px;
    border-radius: 999px;
    border: 1px solid #d8e3f7;
    background: #fff;
    color: #102033;
}

.hero {
    max-width: 980px;
    margin: 0 auto;
    padding: 128px 24px 112px;
    text-align: center;
}

.eyebrow,
.section-title span {
    color: #2563eb;
    font-size: 14px;
    font-weight: 700;
}

.hero h1 {
    margin: 18px 0 20px;
    font-size: clamp(42px, 7vw, 76px);
    line-height: 1.06;
    letter-spacing: 0;
}

.subtitle {
    max-width: 760px;
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
    max-width: 1120px;
    margin: 0 auto;
    padding: 68px 24px;
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
.scene-grid article {
    min-height: 116px;
    padding: 22px;
    border-radius: 18px;
    background: #fff;
    border: 1px solid rgba(37, 99, 235, 0.1);
    box-shadow: 0 12px 28px rgba(37, 99, 235, 0.06);
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
    border-top: 1px solid rgba(37, 99, 235, 0.1);
    border-bottom: 1px solid rgba(37, 99, 235, 0.1);
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
    background: #fff;
    border: 1px solid rgba(37, 99, 235, 0.1);
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

@media (max-width: 920px) {
    .landing-header {
        padding: 0 20px;
    }

    nav a {
        display: none;
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
    .hero {
        padding-top: 86px;
    }

    .capability-grid,
    .scene-grid,
    .steps {
        grid-template-columns: 1fr;
    }
}
</style>
