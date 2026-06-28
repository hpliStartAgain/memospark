import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowDown, ArrowRight, CalendarRange, Check, Compass, Layers3,
  RefreshCcw, Sparkles, Zap,
} from 'lucide-react'
import AccessPanel from '@/components/AccessPanel'
import { useAppStore } from '@/store/appStore'

const steps = [
  {
    number: '01',
    title: '目标',
    body: '上传真实 JD，先确定岗位需要什么，而不是从一套通用题库里猜重点。',
  },
  {
    number: '02',
    title: '路线',
    body: 'AI 把技能差距排成长期阶段，并给出未来四周、今天可执行的任务。',
  },
  {
    number: '03',
    title: '学习',
    body: '卡片按入门、进阶、实战组织。先建立心智模型，再进入主动回忆。',
  },
  {
    number: '04',
    title: '校准',
    body: '评分、遗忘和面试表现持续改写计划，让时间始终花在最接近目标的地方。',
  },
]

const schedule = [
  { time: '08:40', label: '复习到期卡片', meta: '12 张 · 预计 14 分钟', done: true },
  { time: '20:00', label: 'Kubernetes / 入门', meta: 'Pod → ReplicaSet · 6 张新卡', done: false },
  { time: '20:30', label: '情境练习', meta: '定位一次 Deployment 滚动发布故障', done: false },
]

export default function LandingPage() {
  const { user } = useAppStore()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    if (window.location.hash === '#access') {
      window.setTimeout(() => document.getElementById('access')?.scrollIntoView(), 50)
    }
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className="min-h-screen bg-white text-gray-950">
      <header className={`fixed inset-x-0 top-0 z-50 border-b transition-colors ${
        scrolled ? 'border-gray-200 bg-white/95 backdrop-blur' : 'border-transparent bg-transparent'
      }`}>
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-5 md:px-8">
          <Link to="/landing" className={`flex items-center gap-2 font-semibold ${scrolled ? 'text-gray-950' : 'text-white'}`}>
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-[#c65f3f]">
              <Zap className="h-4 w-4 text-white" />
            </span>
            MemoSpark
          </Link>
          <nav className={`hidden items-center gap-6 text-sm md:flex ${scrolled ? 'text-gray-600' : 'text-gray-200'}`}>
            <a href="#method" className="hover:text-[#c65f3f]">工作方式</a>
            <a href="#learning" className="hover:text-[#c65f3f]">学习闭环</a>
          </nav>
          {user ? (
            <Link to="/" className="rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950">
              进入工作台
            </Link>
          ) : (
            <a href="#access" className="rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-950">
              登录
            </a>
          )}
        </div>
      </header>

      <section className="relative flex min-h-[82svh] items-end overflow-hidden bg-gray-950">
        <img
          src="/memospark-workspace.png"
          alt="MemoSpark 学习计划与技能工作台"
          className="absolute inset-0 h-full w-full object-cover object-center opacity-45"
        />
        <div className="absolute inset-0 bg-gray-950/60" />
        <div className="relative z-10 mx-auto w-full max-w-7xl px-5 pb-14 pt-28 md:px-8 md:pb-20">
          <p className="mb-5 text-sm font-medium text-emerald-300">面向目标岗位的学习系统</p>
          <h1 className="max-w-4xl text-5xl font-semibold leading-[1.04] text-white md:text-7xl">
            MemoSpark
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-gray-200 md:text-xl">
            把一份 JD 变成长期路线、每周目标和今天该学的内容。
            卡片、复习和反馈围绕同一个目标持续更新。
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <a href="#method" className="inline-flex items-center gap-2 rounded-md bg-[#c65f3f] px-5 py-3 text-sm font-semibold text-white hover:bg-[#a94e33]">
              看它如何工作 <ArrowDown className="h-4 w-4" />
            </a>
            <a href="#access" className="inline-flex items-center gap-2 rounded-md border border-white/50 px-5 py-3 text-sm font-semibold text-white hover:bg-white/10">
              直接开始 <ArrowRight className="h-4 w-4" />
            </a>
          </div>
          <div className="mt-12 grid max-w-3xl grid-cols-2 gap-x-8 gap-y-3 border-t border-white/25 pt-5 text-xs text-gray-300 md:grid-cols-4">
            <span>JD 作为事实源</span>
            <span>阶段化卡片</span>
            <span>滚动四周计划</span>
            <span>自适应复习</span>
          </div>
        </div>
      </section>

      <section id="method" className="border-b border-gray-200 bg-[#f7f8f7] py-20 md:py-28">
        <div className="mx-auto max-w-7xl px-5 md:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-semibold text-[#c65f3f]">一条完整路径</p>
            <h2 className="mt-3 max-w-xl text-3xl font-semibold leading-tight md:text-4xl lg:text-[44px]">
              不是多一套题库，是把目标变成下一步。
            </h2>
          </div>
          <div className="mt-14 grid border-y border-gray-300 md:grid-cols-4">
            {steps.map((step, index) => (
              <div key={step.number} className={`py-7 md:px-6 md:py-9 ${index > 0 ? 'border-t border-gray-300 md:border-l md:border-t-0' : ''}`}>
                <span className="font-mono text-sm text-gray-400">{step.number}</span>
                <h3 className="mt-8 text-xl font-semibold">{step.title}</h3>
                <p className="mt-3 text-sm leading-6 text-gray-600">{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="learning" className="py-20 md:py-28">
        <div className="mx-auto grid max-w-7xl gap-12 px-5 md:grid-cols-[0.9fr_1.1fr] md:px-8">
          <div>
            <p className="text-sm font-semibold text-emerald-700">先理解，再记住</p>
            <h2 className="mt-3 max-w-xl text-3xl font-semibold leading-tight md:text-4xl">
              第一次翻开，和之后复习，不该是同一场考试。
            </h2>
            <p className="mt-6 max-w-xl leading-7 text-gray-600">
              首次学习时，AI 负责搭脚手架、指出核心因果并宽容评分；
              进入复习后，它才收紧标准，检查是否达到面试表达的准确度。
            </p>
            <div className="mt-8 space-y-4">
              <div className="flex gap-3">
                <Compass className="mt-0.5 h-5 w-5 text-[#c65f3f]" />
                <div><strong className="font-semibold">阶段顺序</strong><p className="mt-1 text-sm text-gray-600">Pod 之后才是 ReplicaSet，再进入 Deployment 与故障场景。</p></div>
              </div>
              <div className="flex gap-3">
                <RefreshCcw className="mt-0.5 h-5 w-5 text-emerald-700" />
                <div><strong className="font-semibold">复习时机</strong><p className="mt-1 text-sm text-gray-600">AI 给出建议，系统按掌握度和记忆边界约束下一次复习。</p></div>
              </div>
            </div>
          </div>

          <div className="self-start border border-gray-300 bg-[#f7f8f7] p-5 md:p-7 rounded-lg">
            <div className="flex items-center justify-between border-b border-gray-300 pb-5">
              <div>
                <p className="text-xs text-gray-500">今天 · 第 2 周</p>
                <h3 className="mt-1 text-xl font-semibold">基础覆盖：Kubernetes 工作负载</h3>
              </div>
              <CalendarRange className="h-6 w-6 text-emerald-700" />
            </div>
            <div className="divide-y divide-gray-200">
              {schedule.map(item => (
                <div key={item.time} className="grid grid-cols-[56px_24px_1fr] gap-3 py-5">
                  <span className="font-mono text-xs text-gray-400">{item.time}</span>
                  <span className={`flex h-5 w-5 items-center justify-center rounded border ${item.done ? 'border-emerald-700 bg-emerald-700 text-white' : 'border-gray-300'}`}>
                    {item.done && <Check className="h-3.5 w-3.5" />}
                  </span>
                  <div>
                    <p className="font-medium">{item.label}</p>
                    <p className="mt-1 text-sm text-gray-500">{item.meta}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex items-center justify-between border-t border-gray-300 pt-5 text-sm">
              <span className="text-gray-500">本周完成 42%</span>
              <span className="font-medium text-[#c65f3f]">计划会根据表现更新</span>
            </div>
          </div>
        </div>
      </section>

      <section id="access" className="border-t border-gray-200 bg-[#eaf1ee] py-20 md:py-28">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-5 md:grid-cols-2 md:px-8">
          <div>
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-gray-950 text-white">
              <Layers3 className="h-5 w-5" />
            </div>
            <h2 className="mt-6 text-3xl font-semibold leading-tight md:text-5xl">
              从你的目标岗位开始。
            </h2>
            <p className="mt-5 max-w-lg leading-7 text-gray-600">
              登录后上传 JD、校准技能和时间预算。MemoSpark 会把剩下的工作排成一条可以每天完成的路线。
            </p>
            <div className="mt-7 flex items-center gap-2 text-sm text-gray-600">
              <Sparkles className="h-4 w-4 text-[#c65f3f]" />
              AI 设置和数据都由你掌控
            </div>
          </div>
          {user ? (
            <div className="border border-gray-300 bg-white p-8 shadow-[0_18px_60px_rgba(15,23,42,0.10)] rounded-lg">
              <p className="text-sm text-gray-500">已登录为 {user.username}</p>
              <h3 className="mt-2 text-2xl font-semibold">继续今天的计划</h3>
              <Link to="/" className="mt-6 inline-flex items-center gap-2 rounded-md bg-gray-950 px-5 py-3 text-sm font-semibold text-white">
                进入工作台 <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          ) : <AccessPanel />}
        </div>
      </section>

      <footer className="bg-gray-950 py-8 text-gray-400">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-5 text-sm md:flex-row md:items-center md:justify-between md:px-8">
          <span className="font-medium text-white">MemoSpark</span>
          <span>目标、计划、学习、复习，在同一条路线上。</span>
        </div>
      </footer>
    </div>
  )
}
