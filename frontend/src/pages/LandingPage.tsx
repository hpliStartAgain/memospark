import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Zap, Target, BookOpen, Code2, BarChart3, Sparkles, ArrowRight,
  Layers, Brain, Repeat, Trophy, Bot, ShieldCheck, CheckCircle2,
} from 'lucide-react'
import { useAppStore } from '@/store/appStore'

/* 视差背景层：用 CSS 渐变模拟固定背景，避免外部资源依赖 */
const heroBg = {
  backgroundImage:
    'radial-gradient(1200px 600px at 80% 10%, rgba(218,119,86,0.18), transparent 60%),' +
    'radial-gradient(900px 500px at 10% 90%, rgba(165,77,50,0.16), transparent 60%),' +
    'linear-gradient(180deg, #f9f8f6 0%, #faece7 100%)',
}
const sectionBgDark = {
  backgroundImage:
    'radial-gradient(1000px 500px at 20% 20%, rgba(218,119,86,0.22), transparent 60%),' +
    'linear-gradient(180deg, #1a1918 0%, #26201d 100%)',
}
const sectionBgLight = {
  backgroundImage:
    'linear-gradient(180deg, #faece7 0%, #f4dad1 100%)',
}
const ctaBg = {
  backgroundImage:
    'radial-gradient(800px 400px at 50% 50%, rgba(218,119,86,0.30), transparent 70%),' +
    'linear-gradient(135deg, #a54d32 0%, #da7756 100%)',
}

function PillButton({ children, onClick, to, variant = 'solid' }: {
  children: React.ReactNode
  onClick?: () => void
  to?: string
  variant?: 'solid' | 'glass'
}) {
  const cls =
    'inline-flex items-center justify-center gap-2 px-6 py-3 rounded-full font-medium ' +
    'backdrop-blur-md transition-all duration-300 active:scale-95 ' +
    (variant === 'solid'
      ? 'bg-primary-600 hover:bg-primary-700 text-white shadow-lg hover:shadow-xl hover:-translate-y-0.5'
      : 'bg-white/70 dark:bg-white/10 hover:bg-white/90 text-primary-700 dark:text-white border border-white/60 dark:border-white/20 shadow-md hover:-translate-y-0.5')
  if (to) return <Link to={to} className={cls}>{children}</Link>
  return <button onClick={onClick} className={cls}>{children}</button>
}

function FeatureCard({ icon: Icon, title, desc }: {
  icon: React.ElementType; title: string; desc: string
}) {
  return (
    <div className="backdrop-blur-sm rounded-2xl p-8 md:p-10 bg-white/80 dark:bg-gray-900/70 border border-white/60 dark:border-gray-800/70 shadow-lg transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:border-primary-300/60 dark:hover:border-primary-700/50">
      <div className="w-12 h-12 rounded-2xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center mb-5">
        <Icon className="w-6 h-6 text-primary-600 dark:text-primary-400" />
      </div>
      <h3 className="font-bold tracking-tight text-xl md:text-2xl text-gray-900 dark:text-white mb-2">{title}</h3>
      <p className="text-sm md:text-base text-gray-600 dark:text-gray-300 leading-relaxed">{desc}</p>
    </div>
  )
}

function ShowcaseRow({ icon: Icon, title, desc, bullets, reverse, dark } : {
  icon: React.ElementType; title: string; desc: string
  bullets: string[]
  reverse?: boolean
  dark?: boolean
}) {
  return (
    <div className={`grid md:grid-cols-2 gap-10 md:gap-16 items-center ${reverse ? 'md:[&>*:first-child]:order-2' : ''}`}>
      <div className="fade-in-up">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-xs font-medium mb-4">
          <Icon className="w-3.5 h-3.5" /> 产品能力
        </div>
        <h3 className={`font-bold tracking-tight text-3xl md:text-4xl mb-4 ${dark ? 'text-white' : 'text-gray-900 dark:text-white'}`}>{title}</h3>
        <p className={`text-base md:text-lg leading-relaxed mb-6 max-w-xl ${dark ? 'text-gray-200' : 'text-gray-600 dark:text-gray-300'}`}>{desc}</p>
        <ul className="space-y-2.5">
          {bullets.map(b => (
            <li key={b} className={`flex items-start gap-2.5 text-sm md:text-base ${dark ? 'text-gray-200' : 'text-gray-700 dark:text-gray-300'}`}>
              <CheckCircle2 className="w-5 h-5 text-primary-500 shrink-0 mt-0.5" />
              <span>{b}</span>
            </li>
          ))}
        </ul>
      </div>
      <div className="fade-in-up">
        <div className={`backdrop-blur-sm rounded-2xl p-8 md:p-10 border shadow-2xl transition-all duration-300 hover:-translate-y-1 ${
          dark ? 'bg-white/10 border-white/20' : 'bg-white/80 dark:bg-gray-900/70 border-white/60 dark:border-gray-800/70'
        }`}>
          <Icon className={`w-16 h-16 mb-4 ${dark ? 'text-white' : 'text-primary-600 dark:text-primary-400'}`} />
          <div className="space-y-3">
            {[0, 1, 2].map(i => (
              <div key={i} className={`h-3 rounded-full ${dark ? 'bg-white/20' : 'bg-gray-200 dark:bg-gray-700'}`}
                style={{ width: `${[100, 85, 70][i]}%` }} />
            ))}
            <div className="flex gap-2 pt-3">
              {[0, 1, 2].map(i => (
                <div key={i} className="flex-1 h-20 rounded-xl bg-primary-500/30 border border-primary-400/40 flex items-end p-2">
                  <div className="h-2 w-full rounded-full bg-primary-500/60" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function LandingPage() {
  const navigate = useNavigate()
  const { user } = useAppStore()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const enterHref = user ? '/' : '/login'
  const enterLabel = user ? '进入工作台' : '免费开始'

  return (
    <div className="min-h-screen bg-background-light dark:bg-gray-950 text-gray-900 dark:text-gray-100">
      {/* 顶部导航：滚动后玻璃态 */}
      <header className={`fixed top-0 inset-x-0 z-50 transition-all duration-300 ${
        scrolled
          ? 'bg-white/80 dark:bg-gray-950/80 backdrop-blur-md border-b border-gray-200/60 dark:border-gray-800/60 shadow-sm'
          : 'bg-transparent'
      }`}>
        <div className="max-w-6xl mx-auto px-6 md:px-12 h-16 flex items-center justify-between">
          <Link to="/landing" className="flex items-center gap-2 font-bold tracking-tight text-lg">
            <span className="w-8 h-8 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-md">
              <Zap className="w-4.5 h-4.5 text-white" />
            </span>
            <span className="text-gray-900 dark:text-white">MemoSpark</span>
          </Link>
          <nav className="hidden md:flex items-center gap-7 text-sm text-gray-600 dark:text-gray-300">
            <a href="#features" className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">功能</a>
            <a href="#workflow" className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">工作流</a>
            <a href="#ai" className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">AI 集成</a>
            <a href="#stats" className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">数据</a>
          </nav>
          <div className="flex items-center gap-2">
            <PillButton to={enterHref} variant="glass">{enterLabel}</PillButton>
          </div>
        </div>
      </header>

      {/* Hero：左右布局 + 视差固定背景 */}
      <section className="parallax-bg min-h-screen flex items-center px-6 md:px-12 pt-16" style={heroBg}>
        <div className="max-w-6xl mx-auto w-full grid md:grid-cols-2 gap-12 items-center">
          <div className="fade-in-up">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/70 dark:bg-white/10 backdrop-blur-md border border-white/60 dark:border-white/20 text-xs font-medium text-primary-700 dark:text-primary-300 mb-6">
              <Sparkles className="w-3.5 h-3.5" /> 目标驱动的面试记忆引擎
            </div>
            <h1 className="font-bold tracking-tight text-5xl md:text-7xl leading-[1.05] text-gray-900 dark:text-white mb-6">
              把八股与刷题<br />
              <span className="text-primary-600 dark:text-primary-400">沉淀成长期记忆</span>
            </h1>
            <p className="text-lg md:text-xl text-gray-600 dark:text-gray-300 leading-relaxed max-w-xl mb-8">
              MemoSpark 用间隔重复算法管理闪卡与错题，结合 LeetCode Hot 100 刷题、AI 评分与「目标岗位」就绪度跟踪，
              让每一次复习都精准命中你的薄弱点。
            </p>
            <div className="flex flex-wrap items-center gap-3">
              <PillButton to={enterHref}>
                {enterLabel} <ArrowRight className="w-4 h-4" />
              </PillButton>
              <PillButton to="/login" variant="glass">登录账号</PillButton>
            </div>
            <div className="flex items-center gap-6 mt-8 text-xs text-gray-500 dark:text-gray-400">
              <span className="flex items-center gap-1.5"><ShieldCheck className="w-4 h-4 text-primary-500" /> 本地部署 · 数据自持</span>
              <span className="flex items-center gap-1.5"><Bot className="w-4 h-4 text-primary-500" /> MCP / AI 助手集成</span>
            </div>
          </div>

          {/* 右侧产品视觉：就绪度环 + 技能条示意 */}
          <div className="fade-in-up hidden md:block">
            <div className="backdrop-blur-md rounded-2xl bg-white/70 dark:bg-gray-900/60 border border-white/60 dark:border-gray-800/60 shadow-2xl p-8">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">主目标就绪度</p>
                  <p className="font-bold text-lg text-gray-900 dark:text-white">高级前端工程师</p>
                </div>
                <div className="relative w-20 h-20">
                  <svg width="80" height="80" className="-rotate-90">
                    <circle cx="40" cy="40" r="34" fill="none" strokeWidth="7" className="stroke-gray-200 dark:stroke-gray-700" />
                    <circle cx="40" cy="40" r="34" fill="none" strokeWidth="7" stroke="#da7756" strokeLinecap="round"
                      strokeDasharray={2 * Math.PI * 34} strokeDashoffset={2 * Math.PI * 34 * (1 - 0.72)}
                      style={{ transition: 'stroke-dashoffset 1s ease' }} />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center font-bold text-gray-900 dark:text-white">72</div>
                </div>
              </div>
              <div className="space-y-3">
                {[
                  { n: 'React 原理', v: 85 }, { n: '浏览器渲染', v: 64 },
                  { n: '工程化', v: 78 }, { n: '算法', v: 52 },
                ].map(s => (
                  <div key={s.n}>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-700 dark:text-gray-300">{s.n}</span>
                      <span className="text-gray-400">{s.v}%</span>
                    </div>
                    <div className="h-2 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                      <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-primary-700" style={{ width: `${s.v}%` }} />
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-6 grid grid-cols-3 gap-3 text-center">
                {[
                  { k: '连续', v: '23 天' },
                  { k: '今日复习', v: '18 张' },
                  { k: '保留率', v: '91%' },
                ].map(x => (
                  <div key={x.k} className="rounded-xl bg-white/60 dark:bg-gray-800/60 p-3">
                    <p className="font-bold text-gray-900 dark:text-white">{x.v}</p>
                    <p className="text-[11px] text-gray-500 dark:text-gray-400">{x.k}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 功能矩阵 */}
      <section id="features" className="py-24 md:py-32 px-6 md:px-12 bg-background-light dark:bg-gray-950">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-14 md:mb-20 fade-in-up">
            <h2 className="font-bold tracking-tight text-4xl md:text-5xl text-gray-900 dark:text-white mb-4">一个平台，覆盖备考全链路</h2>
            <p className="text-lg md:text-xl text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">从目标拆解到记忆巩固，从刷题实战到模拟面试，每一步都被数据驱动。</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 md:gap-8">
            <FeatureCard icon={Target} title="目标岗位管理" desc="录入 JD，AI 自动拆解技能图谱与权重，按技能自评计算面试就绪度，距面试倒计时一目了然。" />
            <FeatureCard icon={Repeat} title="间隔重复复习" desc="基于 SRS 算法的闪卡牌组，自动安排到期卡片，聚焦遗忘曲线临界点，八股越背越牢。" />
            <FeatureCard icon={Code2} title="LeetCode 刷题" desc="内置 Hot 100 题库，Monaco 编辑器在线编写，Judge0 容器评测实时反馈通过率与用例。" />
            <FeatureCard icon={Brain} title="错题本巩固" desc="刷题错题自动入库，按知识点归类并安排重做周期，薄弱点反复攻克直到掌握。" />
            <FeatureCard icon={Bot} title="AI 评分与提示" desc="代码提交后 AI 流式点评给出评分与优化建议；复习卡支持 AI 生成解释，理解先于记忆。" />
            <FeatureCard icon={BarChart3} title="数据看板" desc="连续学习天数、记忆保留率、各技能进度可视化，用数据见证成长曲线。" />
          </div>
        </div>
      </section>

      {/* 工作流：左右交替视差段 */}
      <section id="workflow" className="parallax-bg py-24 md:py-32 px-6 md:px-12" style={sectionBgLight}>
        <div className="max-w-6xl mx-auto space-y-24 md:space-y-32">
          <ShowcaseRow icon={Target}
            title="从 JD 到技能图谱，3 分钟拆解目标"
            desc="粘贴目标岗位 JD，AI 提取关键技能并按重要性赋权。逐项自评当前水平，系统加权计算就绪度，告诉你离面试还差多远。"
            bullets={['AI 自动提取技能与权重', '1–5 级自评 + 加权就绪度', '面试日期倒计时提醒']} />
          <ShowcaseRow icon={Repeat} reverse
            title="闪卡 + 错题双轨记忆"
            desc="八股文走闪卡牌组，刷题错题走错题本。两套 SRS 调度独立运行，到期卡片聚合到今日任务，打开就能直接开背。"
            bullets={['牌组每日限额可调', '错题按知识点归类', '到期卡片一键聚合']} />
          <ShowcaseRow icon={Trophy}
            title="刷题—评测—巩固闭环"
            desc="在线写代码、容器评测、AI 点评、错题入库、间隔重做。一次刷题沉淀为长期记忆，而不是做完就忘。"
            bullets={['Monaco 编辑器 + 多语言评测', 'AI 流式点评与评分', '错题自动入库安排重做']} />
        </div>
      </section>

      {/* AI 集成：深色视差段 */}
      <section id="ai" className="parallax-bg py-24 md:py-32 px-6 md:px-12 text-white" style={sectionBgDark}>
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-2 gap-12 md:gap-16 items-center">
            <div className="fade-in-up">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-xs font-medium mb-4">
                <Bot className="w-3.5 h-3.5" /> MCP Server
              </div>
              <h3 className="font-bold tracking-tight text-3xl md:text-4xl mb-4">把 AI 助手变成你的记忆外挂</h3>
              <p className="text-base md:text-lg leading-relaxed text-gray-200 mb-6 max-w-xl">
                内置 MCP Server 让 Claude Desktop、Windsurf 等 AI 助手直接读写你的牌组。
                在对话中遇到值得复习的概念，一句话即可生成闪卡入库，复习节奏由 SRS 自动接管。
              </p>
              <ul className="space-y-2.5">
                {['add_flashcard / list_decks / get_due_cards 等工具', 'Bearer Key 鉴权的 Quick-Add REST API', '兼容 OpenAI / DeepSeek / 阿里云等 LLM 接口'].map(b => (
                  <li key={b} className="flex items-start gap-2.5 text-gray-200">
                    <CheckCircle2 className="w-5 h-5 text-primary-400 shrink-0 mt-0.5" />
                    <span>{b}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="fade-in-up">
              <div className="backdrop-blur-md rounded-2xl bg-gray-900/60 border border-white/15 shadow-2xl p-6 font-mono text-sm overflow-hidden">
                <div className="flex items-center gap-1.5 mb-4">
                  <span className="w-3 h-3 rounded-full bg-red-400/80" />
                  <span className="w-3 h-3 rounded-full bg-yellow-400/80" />
                  <span className="w-3 h-3 rounded-full bg-green-400/80" />
                </div>
                <pre className="text-gray-300 leading-relaxed whitespace-pre-wrap"><span className="text-primary-400">user</span>: 帮我把这条加进八股牌组
{"{"}"q": "HTTPS 握手过程", "a": "..."{"}"}
<span className="text-primary-400">assistant</span>: 已调用 add_flashcard，
新卡已加入「八股」牌组，
明天到期复习。
<span className="text-gray-500"># SRS 自动安排下次复习</span></pre>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 数据统计视差段 */}
      <section id="stats" className="py-24 md:py-32 px-6 md:px-12 bg-background-light dark:bg-gray-950">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-14 fade-in-up">
            <h2 className="font-bold tracking-tight text-4xl md:text-5xl text-gray-900 dark:text-white mb-4">用数据见证每一寸进步</h2>
            <p className="text-lg text-gray-600 dark:text-gray-300">可视化你的备考旅程，让坚持可被衡量。</p>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
            {[
              { icon: Layers, v: '100+', k: '内置 LeetCode Hot 题目' },
              { icon: Repeat, v: 'SRS', k: '间隔重复算法调度' },
              { icon: Brain, v: '∞', k: '自建闪卡与错题' },
              { icon: ShieldCheck, v: 'PWA', k: '离线可安装 · 多端同步' },
            ].map(x => (
              <div key={x.k} className="backdrop-blur-sm rounded-2xl bg-white/80 dark:bg-gray-900/70 border border-white/60 dark:border-gray-800/70 shadow-lg p-6 md:p-8 text-center transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl">
                <x.icon className="w-8 h-8 mx-auto mb-3 text-primary-600 dark:text-primary-400" />
                <p className="font-bold text-3xl md:text-4xl text-gray-900 dark:text-white">{x.v}</p>
                <p className="text-xs md:text-sm text-gray-500 dark:text-gray-400 mt-1">{x.k}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA 收尾：视差固定背景 */}
      <section className="parallax-bg py-24 md:py-32 px-6 md:px-12 text-white" style={ctaBg}>
        <div className="max-w-3xl mx-auto text-center fade-in-up">
          <h2 className="font-bold tracking-tight text-4xl md:text-5xl mb-5">现在开始，让备考有的放矢</h2>
          <p className="text-lg md:text-xl text-white/90 mb-8 max-w-xl mx-auto">免费部署到本地，数据完全自持。几分钟搭建专属面试记忆引擎。</p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <PillButton to={enterHref}>
              {enterLabel} <ArrowRight className="w-4 h-4" />
            </PillButton>
            <PillButton to="/login" variant="glass">登录账号</PillButton>
          </div>
        </div>
      </section>

      {/* 页脚 */}
      <footer className="py-12 md:py-16 px-6 md:px-12 bg-gray-950 text-gray-400">
        <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-7 h-7 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                <Zap className="w-4 h-4 text-white" />
              </span>
              <span className="font-bold text-white text-lg">MemoSpark</span>
            </div>
            <p className="text-sm">目标驱动的程序员面试备考：岗位/JD 技能分析、刷题、八股文间隔复习与就绪度跟踪。</p>
          </div>
          <div>
            <h4 className="font-bold text-white mb-3">产品</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#features" className="hover:text-primary-400 transition-colors">功能矩阵</a></li>
              <li><a href="#workflow" className="hover:text-primary-400 transition-colors">工作流</a></li>
              <li><a href="#ai" className="hover:text-primary-400 transition-colors">AI / MCP 集成</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-bold text-white mb-3">快速入口</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/login" className="hover:text-primary-400 transition-colors">登录</Link></li>
              <li><Link to="/password-reset" className="hover:text-primary-400 transition-colors">找回密码</Link></li>
              <li><button onClick={() => navigate(enterHref)} className="hover:text-primary-400 transition-colors">进入工作台</button></li>
            </ul>
          </div>
        </div>
        <div className="max-w-6xl mx-auto mt-10 pt-6 border-t border-gray-800 text-xs text-gray-500">
          © {new Date().getFullYear()} MemoSpark · Built with React · Tailwind · Spring Boot
        </div>
      </footer>
    </div>
  )
}
