<template>
  <div class="dashboard-container">
    <canvas ref="particleCanvas" class="particle-bg"></canvas>
    <div class="gradient-overlay"></div>
    
    <div class="top-bar">
      <div class="logo-area">
        <div class="logo-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="white" stroke-width="1.5" fill="rgba(59,130,246,0.2)"/>
            <path d="M2 17L12 22L22 17" stroke="white" stroke-width="1.5" fill="rgba(59,130,246,0.2)"/>
            <path d="M2 12L12 17L22 12" stroke="white" stroke-width="1.5" fill="rgba(59,130,246,0.2)"/>
          </svg>
        </div>
        <div class="logo-text">
          <span>慧维智能运维平台</span>
          <span class="version">v1.0</span>
        </div>
      </div>
      
      <div class="header-stats">
        <div class="stat-chip">
          <div class="chip-icon">📊</div>
          <div><span>健康度</span><strong>96%</strong></div>
        </div>
        <div class="stat-chip">
          <div class="chip-icon">⏱️</div>
          <div><span>响应时间</span><strong>128ms</strong></div>
        </div>
        <div class="stat-chip">
          <div class="chip-icon">💻</div>
          <div><span>总机器数</span><strong>{{ machines.length }}</strong></div>
        </div>
        <div class="stat-chip warning">
          <div class="chip-icon">⚠️</div>
          <div><span>告警中</span><strong>{{ alertCount }}</strong></div>
        </div>
      </div>
      
      <div class="right-info">
        <div class="time-display">{{ currentTime }}</div>
        <div class="user-display">{{ userName }}</div>
        <div class="exit-btn" @click="goToMachineList">✕</div>
      </div>
    </div>

    <div class="dashboard-main">
      <div class="cluster-panel">
        <div class="panel-header">
          <h3>机器集群</h3>
          <span class="total-badge">{{ machines.length }}台在线</span>
        </div>
        <div class="machines-scroll">
          <div 
            v-for="machine in machines" 
            :key="machine.id" 
            class="machine-item"
            :class="{ active: activeMachine === machine.id, warning: machine.status === 'warning' }"
            @click="selectMachine(machine.id)"
          >
            <div class="machine-status" :class="machine.status"></div>
            <div class="machine-avatar">
              <div class="avatar-icon">{{ machine.name.charAt(0) }}</div>
            </div>
            <div class="machine-info">
              <div class="machine-name">{{ machine.name }}</div>
              <div class="machine-ip">{{ machine.ip }}</div>
            </div>
            <div class="machine-metrics">
              <div class="mini-metric">
                <span>CPU</span>
                <strong :class="getCpuClass(machine.cpu)">{{ Math.floor(machine.cpu) }}%</strong>
              </div>
              <div class="mini-metric">
                <span>内存</span>
                <strong :class="getMemClass(machine.memory)">{{ Math.floor(machine.memory) }}%</strong>
              </div>
            </div>
            <div class="machine-actions">
              <button class="action-btn" title="终端">📟</button>
              <button class="action-btn" title="SFTP">📁</button>
              <button class="action-btn" title="更多">⋯</button>
            </div>
            <div class="machine-load">
              <div class="load-bar" :style="{ width: machine.cpu + '%', background: getCpuColor(machine.cpu) }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-panel">
        <div class="panel-header">
          <h3>{{ currentMachine.name }} - 实时监控</h3>
          <div class="refresh-badge">实时更新</div>
        </div>
        
        <div class="metrics-gauge-row">
          <div class="gauge-item">
            <div class="gauge-container">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" stroke="rgba(255,255,255,0.1)" stroke-width="8" fill="none"/>
                <circle cx="50" cy="50" r="42" stroke="url(#cpuGrad)" stroke-width="8" fill="none"
                        :stroke-dasharray="`${currentMachine.cpu * 2.64}, 264`" stroke-linecap="round"
                        transform="rotate(-90 50 50)"/>
                <defs><linearGradient id="cpuGrad"><stop offset="0%" stop-color="#3b82f6"/><stop offset="100%" stop-color="#06b6d4"/></linearGradient></defs>
              </svg>
              <div class="gauge-value">{{ Math.floor(currentMachine.cpu) }}<span>%</span></div>
              <span class="gauge-label">CPU使用率</span>
            </div>
          </div>
          <div class="gauge-item">
            <div class="gauge-container">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" stroke="rgba(255,255,255,0.1)" stroke-width="8" fill="none"/>
                <circle cx="50" cy="50" r="42" stroke="url(#memGrad)" stroke-width="8" fill="none"
                        :stroke-dasharray="`${currentMachine.memory * 2.64}, 264`" stroke-linecap="round"
                        transform="rotate(-90 50 50)"/>
                <defs><linearGradient id="memGrad"><stop offset="0%" stop-color="#10b981"/><stop offset="100%" stop-color="#34d399"/></linearGradient></defs>
              </svg>
              <div class="gauge-value">{{ Math.floor(currentMachine.memory) }}<span>%</span></div>
              <span class="gauge-label">内存使用率</span>
            </div>
          </div>
          <div class="gauge-item">
            <div class="gauge-container">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" stroke="rgba(255,255,255,0.1)" stroke-width="8" fill="none"/>
                <circle cx="50" cy="50" r="42" stroke="url(#netGrad)" stroke-width="8" fill="none"
                        :stroke-dasharray="`${(currentMachine.network / 10) * 264}, 264`" stroke-linecap="round"
                        transform="rotate(-90 50 50)"/>
                <defs><linearGradient id="netGrad"><stop offset="0%" stop-color="#f59e0b"/><stop offset="100%" stop-color="#f97316"/></linearGradient></defs>
              </svg>
              <div class="gauge-value">{{ currentMachine.network.toFixed(1) }}<span>M</span></div>
              <span class="gauge-label">网络吞吐量</span>
            </div>
          </div>
        </div>
        
        <div class="system-info">
          <div class="info-row">
            <span>主机名</span>
            <strong>{{ currentMachine.name }}</strong>
          </div>
          <div class="info-row">
            <span>IP地址</span>
            <strong>{{ currentMachine.ip }}</strong>
          </div>
          <div class="info-row">
            <span>操作系统</span>
            <strong>{{ currentMachine.os }}</strong>
          </div>
          <div class="info-row">
            <span>运行时间</span>
            <strong>{{ currentMachine.uptime }}</strong>
          </div>
          <div class="info-row">
            <span>CPU核心数</span>
            <strong>{{ currentMachine.cpuCores }}核</strong>
          </div>
          <div class="info-row">
            <span>总内存</span>
            <strong>{{ currentMachine.totalMemory }}</strong>
          </div>
        </div>
        
        <div class="disk-detail">
          <div class="detail-title">磁盘使用情况</div>
          <div class="disk-grid">
            <div v-for="disk in currentMachine.disks" :key="disk.name" class="disk-card">
              <div class="disk-header"><span>{{ disk.name }}</span><span>{{ disk.usage }}%</span></div>
              <div class="disk-bar-large"><div class="disk-fill" :style="{ width: disk.usage + '%', background: disk.usage > 80 ? '#f87171' : disk.usage > 60 ? '#fbbf24' : '#3b82f6' }"></div></div>
              <div class="disk-space"><span>已用 {{ disk.used }}</span><span>可用 {{ disk.free }}</span></div>
            </div>
          </div>
        </div>
        
        <div class="process-detail">
          <div class="detail-title">
            <span>进程 TOP5</span>
            <span class="process-total">共 {{ currentMachine.processes.length }} 个进程</span>
          </div>
          <div class="process-list-large">
            <div v-for="(proc, idx) in currentMachine.processes" :key="proc.pid" class="process-row">
              <div class="proc-rank">{{ idx + 1 }}</div>
              <div class="proc-name">{{ proc.name }}</div>
              <div class="proc-bar-container">
                <div class="proc-bar">
                  <div class="proc-fill" :style="{ width: proc.cpu + '%' }"></div>
                </div>
                <span class="proc-percent">{{ Math.floor(proc.cpu) }}%</span>
              </div>
              <div class="proc-status" :class="proc.cpu > 30 ? 'high' : proc.cpu > 15 ? 'medium' : 'low'">
                {{ proc.cpu > 30 ? '高负载' : proc.cpu > 15 ? '中负载' : '低负载' }}
              </div>
            </div>
          </div>
          <div class="process-summary">
            <div class="summary-item">
              <span>平均CPU</span>
              <strong>{{ avgProcessCpu }}%</strong>
            </div>
            <div class="summary-item">
              <span>总内存占用</span>
              <strong>{{ totalProcessMem }}MB</strong>
            </div>
            <div class="summary-item">
              <span>高负载进程</span>
              <strong class="warning">{{ highLoadCount }}</strong>
            </div>
          </div>
        </div>
      </div>

      <div class="events-panel">
        <div class="panel-header">
          <h3>实时事件流</h3>
          <div class="events-control">
            <span class="live-badge">LIVE</span>
            <div class="scroll-control" @click="toggleScroll">
              {{ autoScroll ? '⏸' : '▶' }}
            </div>
          </div>
        </div>
        
        <div class="events-stats">
          <div class="stat-item">
            <span>今日事件</span>
            <strong>{{ events.length }}</strong>
          </div>
          <div class="stat-item">
            <span>告警事件</span>
            <strong class="warning">{{ alertEventsCount }}</strong>
          </div>
          <div class="stat-item">
            <span>构建事件</span>
            <strong>{{ buildEventsCount }}</strong>
          </div>
        </div>
        
        <div class="event-list" ref="eventList" :class="{ 'paused': !autoScroll }">
          <div v-for="event in events" :key="event.id" class="event-item">
            <div class="event-time">{{ event.time }}</div>
            <div class="event-badge" :class="event.type">{{ event.typeName }}</div>
            <div class="event-message">{{ event.message }}</div>
            <div class="event-machine">{{ event.machine }}</div>
          </div>
          <div v-if="events.length === 0" class="empty-state">暂无事件</div>
        </div>
        
        <div class="alert-summary">
          <div class="alert-header">
            <span>⚠️ 实时告警</span>
            <span class="alert-count">{{ alerts.length }}</span>
          </div>
          <div class="alert-list">
            <div v-for="alert in alerts.slice(0, 3)" :key="alert.id" class="alert-item" :class="alert.level">
              <div class="alert-content">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-desc">{{ alert.description }}</div>
              </div>
              <div class="alert-time">{{ alert.time }}</div>
            </div>
            <div v-if="alerts.length === 0" class="empty-alerts">暂无告警</div>
          </div>
        </div>
        
        <div class="quick-actions">
          <div class="quick-title">快速操作</div>
          <div class="actions-grid">
            <div v-for="action in quickActions.slice(0, 4)" :key="action.name" class="action-chip" @click="goTo(action.path)">
              <span class="action-icon">{{ action.iconEmoji }}</span>
              <span>{{ action.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-actions">
      <div v-for="action in quickActions" :key="action.name" class="action-block" @click="goTo(action.path)">
        <div class="action-icon" :style="{ background: action.bg }">
          <a-icon :type="action.icon" />
        </div>
        <span class="action-name">{{ action.name }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'

export default {
  name: 'Console',
  data() {
    return {
      userName: '',
      currentTime: dayjs().format('HH:mm:ss'),
      timer: null,
      scrollTimer: null,
      eventTimer: null,
      dataTimer: null,
      autoScroll: true,
      activeMachine: 1,
      events: [],
      alerts: [],
      machines: [
        {
          id: 1, name: 'MJH', status: 'healthy', ip: '192.168.68.80', os: 'Windows Server 2022', uptime: '15天 6小时', cpuCores: 8, totalMemory: '32GB',
          cpu: 45, memory: 62, network: 3.2,
          disks: [
            { name: 'C:', usage: 45, used: '128GB', free: '156GB' },
            { name: 'D:', usage: 62, used: '310GB', free: '190GB' }
          ],
          processes: [
            { pid: 1234, name: 'java.exe', cpu: 32, mem: 512 },
            { pid: 5678, name: 'nginx.exe', cpu: 15, mem: 128 },
            { pid: 9012, name: 'mysql.exe', cpu: 12, mem: 256 },
            { pid: 3456, name: 'redis-server.exe', cpu: 8, mem: 64 },
            { pid: 7890, name: 'docker.exe', cpu: 6, mem: 384 }
          ]
        },
        {
          id: 2, name: '吴雨树', status: 'warning', ip: '192.168.68.112', os: 'Windows Server 2022', uptime: '8天 12小时', cpuCores: 16, totalMemory: '64GB',
          cpu: 78, memory: 85, network: 5.6,
          disks: [
            { name: 'C:', usage: 78, used: '156GB', free: '44GB' },
            { name: 'D:', usage: 85, used: '425GB', free: '75GB' }
          ],
          processes: [
            { pid: 2234, name: 'java.exe', cpu: 45, mem: 1024 },
            { pid: 6678, name: 'node.exe', cpu: 22, mem: 384 },
            { pid: 1012, name: 'python.exe', cpu: 12, mem: 256 },
            { pid: 4456, name: 'postgres.exe', cpu: 8, mem: 512 },
            { pid: 8890, name: 'nginx.exe', cpu: 5, mem: 128 }
          ]
        },
        {
          id: 3, name: '郑文杰', status: 'healthy', ip: '192.168.68.29', os: 'Windows Server 2022', uptime: '3天 5小时', cpuCores: 12, totalMemory: '48GB',
          cpu: 32, memory: 48, network: 1.8,
          disks: [
            { name: 'C:', usage: 35, used: '350GB', free: '650GB' },
            { name: 'D:', usage: 42, used: '420GB', free: '580GB' }
          ],
          processes: [
            { pid: 3234, name: 'java.exe', cpu: 18, mem: 768 },
            { pid: 7678, name: 'docker.exe', cpu: 12, mem: 512 },
            { pid: 2012, name: 'kafka.exe', cpu: 8, mem: 1024 },
            { pid: 5456, name: 'elasticsearch.exe', cpu: 15, mem: 2048 },
            { pid: 9890, name: 'logstash.exe', cpu: 5, mem: 256 }
          ]
        },
        {
          id: 4, name: '袁智伟', status: 'healthy', ip: '192.168.68.4', os: 'Windows Server 2022', uptime: '22天 3小时', cpuCores: 8, totalMemory: '32GB',
          cpu: 28, memory: 52, network: 2.4,
          disks: [
            { name: 'C:', usage: 28, used: '112GB', free: '288GB' },
            { name: 'D:', usage: 45, used: '225GB', free: '275GB' }
          ],
          processes: [
            { pid: 4234, name: 'nginx.exe', cpu: 12, mem: 128 },
            { pid: 8678, name: 'php-fpm.exe', cpu: 18, mem: 256 },
            { pid: 3012, name: 'mysql.exe', cpu: 22, mem: 768 },
            { pid: 6456, name: 'redis-server.exe', cpu: 5, mem: 64 },
            { pid: 7891, name: 'prometheus.exe', cpu: 8, mem: 256 }
          ]
        },
        {
          id: 5, name: '尹世光', status: 'warning', ip: '192.168.68.89', os: 'Windows Server 2022', uptime: '5天 18小时', cpuCores: 16, totalMemory: '64GB',
          cpu: 82, memory: 76, network: 4.2,
          disks: [
            { name: 'C:', usage: 82, used: '410GB', free: '90GB' },
            { name: 'D:', usage: 45, used: '450GB', free: '550GB' }
          ],
          processes: [
            { pid: 5234, name: 'java.exe', cpu: 35, mem: 1024 },
            { pid: 9678, name: 'sqlserver.exe', cpu: 28, mem: 2048 },
            { pid: 4012, name: 'iis.exe', cpu: 12, mem: 256 },
            { pid: 7456, name: 'redis-server.exe', cpu: 5, mem: 128 },
            { pid: 1892, name: 'elasticsearch.exe', cpu: 18, mem: 1024 }
          ]
        }
      ],
      quickActions: [
        { name: '批量执行', icon: 'code', iconEmoji: '⚡', path: '/batch/exec/add', bg: '#3b82f6' },
        { name: '日志面板', icon: 'file-text', iconEmoji: '📋', path: '/log/list', bg: '#8b5cf6' },
        { name: '机器终端', icon: 'cloud', iconEmoji: '📟', path: '/machine/terminal', bg: '#10b981' },
        { name: '文件管理', icon: 'folder', iconEmoji: '📁', path: '/machine/sftp', bg: '#f59e0b' },
        { name: '构建任务', icon: 'build', iconEmoji: '🏗️', path: '/app/build/list', bg: '#ef4444' },
        { name: '发布任务', icon: 'rocket', iconEmoji: '🚀', path: '/app/release/list', bg: '#ec489a' },
        { name: '机器列表', icon: 'database', iconEmoji: '💻', path: '/machine/list', bg: '#a855f7' },
        { name: '用户列表', icon: 'team', iconEmoji: '👥', path: '/user/list', bg: '#14b8a6' }
      ]
    }
  },
  computed: {
    onlineCount() { return this.machines.filter(m => m.status === 'healthy').length },
    alertCount() { return this.machines.filter(m => m.status === 'warning').length },
    alertEventsCount() { return this.events.filter(e => e.type === 'warning').length },
    buildEventsCount() { return this.events.filter(e => e.type === 'build').length },
    currentMachine() { return this.machines.find(m => m.id === this.activeMachine) || this.machines[0] },
    avgProcessCpu() {
      const sum = this.currentMachine.processes.reduce((s, p) => s + p.cpu, 0)
      return Math.floor(sum / this.currentMachine.processes.length)
    },
    totalProcessMem() {
      const sum = this.currentMachine.processes.reduce((s, p) => s + p.mem, 0)
      return sum
    },
    highLoadCount() {
      return this.currentMachine.processes.filter(p => p.cpu > 30).length
    }
  },
  mounted() {
    this.userName = this.$storage.get(this.$storage.keys.USER_NAME) || 'Admin'
    this.startTimeUpdate()
    this.initAutoScroll()
    this.initParticleBackground()
    this.startDataUpdate()
    this.startEventSimulation()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
    if (this.scrollTimer) clearInterval(this.scrollTimer)
    if (this.eventTimer) clearInterval(this.eventTimer)
    if (this.dataTimer) clearInterval(this.dataTimer)
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    goToMachineList() { this.$router.push('/machine/list') },
    goTo(path) { this.$router.push(path) },
    selectMachine(id) { this.activeMachine = id },
    startTimeUpdate() {
      this.timer = setInterval(() => {
        this.currentTime = dayjs().format('HH:mm:ss')
      }, 1000)
    },
    initAutoScroll() {
      this.scrollTimer = setInterval(() => {
        if (this.autoScroll && this.$refs.eventList && this.events.length > 0) {
          const el = this.$refs.eventList
          if (el.scrollHeight - el.scrollTop - el.clientHeight < 10) el.scrollTop = el.scrollHeight
          else el.scrollTop += 2
        }
      }, 100)
    },
    toggleScroll() { this.autoScroll = !this.autoScroll },
    initParticleBackground() {
      const canvas = this.$refs.particleCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      let particles = []
      let animationId = null
      const resize = () => {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
        particles = []
        for (let i = 0; i < 120; i++) {
          particles.push({
            x: Math.random() * canvas.width, y: Math.random() * canvas.height,
            radius: Math.random() * 4 + 1, alpha: Math.random() * 0.3 + 0.1,
            speedX: (Math.random() - 0.5) * 0.4, speedY: (Math.random() - 0.5) * 0.3
          })
        }
      }
      const draw = () => {
        if (!ctx) return
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        particles.forEach(p => {
          ctx.beginPath()
          ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
          ctx.fillStyle = `rgba(59, 130, 246, ${p.alpha})`
          ctx.fill()
          p.x += p.speedX; p.y += p.speedY
          if (p.x < 0) p.x = canvas.width
          if (p.x > canvas.width) p.x = 0
          if (p.y < 0) p.y = canvas.height
          if (p.y > canvas.height) p.y = 0
        })
        animationId = requestAnimationFrame(draw)
      }
      window.addEventListener('resize', resize)
      resize()
      draw()
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', resize)
        if (animationId) cancelAnimationFrame(animationId)
      })
    },
    startDataUpdate() {
      this.dataTimer = setInterval(() => {
        this.machines.forEach(machine => {
          machine.cpu = Math.min(100, Math.max(0, machine.cpu + (Math.random() - 0.5) * 5))
          machine.memory = Math.min(100, Math.max(0, machine.memory + (Math.random() - 0.5) * 4))
          machine.network = Math.min(10, Math.max(0, machine.network + (Math.random() - 0.5) * 0.6))
          machine.status = machine.cpu > 80 || machine.memory > 85 ? 'warning' : 'healthy'
          machine.processes.forEach(proc => {
            proc.cpu = Math.min(100, Math.max(0, proc.cpu + (Math.random() - 0.5) * 4))
          })
          machine.processes.sort((a, b) => b.cpu - a.cpu)
        })
      }, 2000)
    },
    addEvent(message, type = 'info', machineName) {
      const typeNames = { build: '构建', release: '发布', login: '登录', terminal: '终端', exec: '执行', info: '信息', warning: '告警' }
      this.events.unshift({
        id: Date.now(), time: dayjs().format('HH:mm:ss'),
        type: type, typeName: typeNames[type] || '信息',
        message: message, machine: machineName || ''
      })
      if (this.events.length > 50) this.events.pop()
    },
    startEventSimulation() {
      this.eventTimer = setInterval(() => {
        const machine = this.machines[Math.floor(Math.random() * this.machines.length)]
        const events = [
          { msg: `应用部署成功`, type: 'build' },
          { msg: `配置更新完成`, type: 'release' },
          { msg: `用户登录系统`, type: 'login' },
          { msg: `批量任务执行完成`, type: 'exec' }
        ]
        const ev = events[Math.floor(Math.random() * events.length)]
        this.addEvent(`${machine.name} ${ev.msg}`, ev.type, machine.name)
        
        if (machine.cpu > 85) {
          this.addEvent(`${machine.name} CPU告警: ${Math.floor(machine.cpu)}%`, 'warning', machine.name)
          this.alerts.unshift({
            id: Date.now(), level: 'warning', title: 'CPU告警',
            description: `${machine.name} CPU使用率 ${Math.floor(machine.cpu)}%`,
            time: dayjs().format('HH:mm:ss')
          })
          if (this.alerts.length > 10) this.alerts.pop()
        }
        if (machine.memory > 88) {
          this.addEvent(`${machine.name} 内存告警: ${Math.floor(machine.memory)}%`, 'warning', machine.name)
          this.alerts.unshift({
            id: Date.now(), level: 'warning', title: '内存告警',
            description: `${machine.name} 内存使用率 ${Math.floor(machine.memory)}%`,
            time: dayjs().format('HH:mm:ss')
          })
          if (this.alerts.length > 10) this.alerts.pop()
        }
      }, 5000)
    },
    getCpuClass(v) { if (v > 80) return 'danger'; if (v > 60) return 'warning'; return 'normal' },
    getMemClass(v) { if (v > 80) return 'danger'; if (v > 60) return 'warning'; return 'normal' },
    getCpuColor(v) { if (v > 80) return '#f87171'; if (v > 60) return '#fbbf24'; return '#3b82f6' },
    handleResize() {}
  }
}
</script>

<style lang="less" scoped>
.dashboard-container {
  position: relative;
  min-height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a0f1e 0%, #020617 100%);
  padding: 20px 24px;
  
  .particle-bg {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
  }
  
  .gradient-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at 80% 20%, rgba(59,130,246,0.08) 0%, transparent 60%);
    pointer-events: none;
    z-index: 0;
  }
}

.top-bar {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 12px 24px;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  
  .logo-area { display: flex; align-items: center; gap: 12px;
    .logo-icon { width: 44px; height: 44px; background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(6,182,212,0.2)); border-radius: 14px; display: flex; align-items: center; justify-content: center; }
    .logo-text { span { font-size: 18px; font-weight: 600; color: #e2e8f0; } .version { font-size: 11px; color: #64748b; margin-left: 8px; } }
  }
  
  .header-stats { display: flex; gap: 16px;
    .stat-chip { display: flex; align-items: center; gap: 10px; padding: 6px 16px; background: rgba(0,0,0,0.3); border-radius: 40px;
      .chip-icon { font-size: 16px; }
      div { display: flex; flex-direction: column; span { font-size: 10px; color: #94a3b8; } strong { font-size: 14px; color: #e2e8f0; } }
      &.warning strong { color: #f87171; }
    }
  }
  
  .right-info { display: flex; align-items: center; gap: 12px;
    .time-display, .user-display { padding: 6px 14px; background: rgba(0,0,0,0.3); border-radius: 30px; font-size: 13px; color: #e2e8f0; }
    .exit-btn { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(239,68,68,0.15); cursor: pointer; color: #f87171; transition: all 0.3s; &:hover { background: rgba(239,68,68,0.3); transform: rotate(90deg); } }
  }
}

.dashboard-main {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: 320px 1fr 360px;
  gap: 20px;
  margin-bottom: 20px;
  
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 10px;
    border-bottom: 1px solid rgba(59,130,246,0.2);
    h3 { font-size: 15px; font-weight: 600; color: #e2e8f0; margin: 0; }
    .total-badge { font-size: 11px; color: #3b82f6; background: rgba(59,130,246,0.15); padding: 2px 8px; border-radius: 20px; }
    .refresh-badge { font-size: 10px; color: #10b981; background: rgba(16,185,129,0.15); padding: 2px 8px; border-radius: 20px; }
    .events-control { display: flex; gap: 8px; align-items: center; .live-badge { background: rgba(239,68,68,0.2); color: #f87171; padding: 2px 8px; border-radius: 20px; font-size: 10px; } .scroll-control { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(59,130,246,0.1); border-radius: 8px; cursor: pointer; &:hover { background: rgba(59,130,246,0.2); } } }
  }
}

.cluster-panel {
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 16px;
  
  .machines-scroll { max-height: calc(100vh - 180px); overflow-y: auto; &::-webkit-scrollbar { width: 4px; } }
  .machine-item {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    padding: 12px;
    margin-bottom: 8px;
    background: rgba(0,0,0,0.3);
    border-radius: 16px;
    cursor: pointer;
    transition: all 0.2s;
    &:hover { background: rgba(59,130,246,0.15); transform: translateX(4px); }
    &.active { background: rgba(59,130,246,0.2); border-left: 3px solid #3b82f6; }
    .machine-status { width: 8px; height: 8px; border-radius: 50%; &.healthy { background: #10b981; box-shadow: 0 0 6px #10b981; } &.warning { background: #fbbf24; box-shadow: 0 0 6px #fbbf24; animation: blink 1s infinite; } }
    .machine-avatar { .avatar-icon { width: 32px; height: 32px; background: linear-gradient(135deg, #3b82f6, #06b6d4); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; color: white; } }
    .machine-info { flex: 1; .machine-name { font-size: 14px; font-weight: 600; color: #e2e8f0; } .machine-ip { font-size: 10px; color: #64748b; } }
    .machine-metrics { display: flex; gap: 12px; .mini-metric { display: flex; gap: 4px; font-size: 10px; color: #94a3b8; strong { font-size: 11px; &.normal { color: #60a5fa; } &.warning { color: #fbbf24; } &.danger { color: #f87171; } } } }
    .machine-actions { display: flex; gap: 6px; .action-btn { background: rgba(0,0,0,0.3); border: none; border-radius: 8px; padding: 4px 6px; color: #94a3b8; cursor: pointer; font-size: 12px; &:hover { background: rgba(59,130,246,0.3); color: #60a5fa; } } }
    .machine-load { width: 100%; margin-top: 8px; .load-bar { height: 3px; border-radius: 2px; transition: width 0.3s; } }
  }
}

.detail-panel {
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 16px;
  
  .metrics-gauge-row { display: flex; justify-content: space-around; margin-bottom: 20px;
    .gauge-item { text-align: center;
      .gauge-container { position: relative; width: 90px; margin: 0 auto; svg { width: 90px; height: 90px; } .gauge-value { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -60%); font-size: 18px; font-weight: 700; color: #e2e8f0; span { font-size: 9px; } } .gauge-label { display: block; font-size: 10px; color: #94a3b8; margin-top: 6px; } }
    }
  }
  
  .system-info { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; background: rgba(0,0,0,0.3); padding: 12px; border-radius: 16px; margin-bottom: 16px;
    .info-row { display: flex; justify-content: space-between; span { font-size: 11px; color: #94a3b8; } strong { font-size: 12px; color: #e2e8f0; } }
  }
  
  .disk-detail { margin-bottom: 16px; .detail-title { font-size: 11px; font-weight: 600; color: #94a3b8; margin-bottom: 10px; } .disk-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; .disk-card { background: rgba(0,0,0,0.3); padding: 10px; border-radius: 12px; .disk-header { display: flex; justify-content: space-between; font-size: 11px; color: #e2e8f0; margin-bottom: 6px; } .disk-bar-large { height: 5px; background: rgba(0,0,0,0.3); border-radius: 3px; overflow: hidden; margin-bottom: 6px; .disk-fill { height: 100%; border-radius: 3px; } } .disk-space { display: flex; justify-content: space-between; font-size: 9px; color: #64748b; } } } }
  
  .process-detail {
    .detail-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-weight: 600;
      color: #94a3b8;
      margin-bottom: 12px;
      .process-total {
        font-size: 10px;
        color: #64748b;
        background: rgba(0,0,0,0.3);
        padding: 2px 8px;
        border-radius: 12px;
      }
    }
    .process-list-large {
      .process-row {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 0;
        border-bottom: 1px solid rgba(71,85,105,0.1);
        .proc-rank {
          width: 32px;
          font-size: 12px;
          font-weight: 600;
          color: #60a5fa;
        }
        .proc-name {
          flex: 1.5;
          font-size: 12px;
          font-weight: 500;
          color: #e2e8f0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .proc-bar-container {
          flex: 3;
          display: flex;
          align-items: center;
          gap: 12px;
          .proc-bar {
            flex: 1;
            height: 8px;
            background: rgba(0,0,0,0.3);
            border-radius: 4px;
            overflow: hidden;
            .proc-fill {
              height: 100%;
              background: linear-gradient(90deg, #fbbf24, #f59e0b);
              border-radius: 4px;
              transition: width 0.3s;
            }
          }
          .proc-percent {
            width: 40px;
            font-size: 11px;
            font-weight: 600;
            color: #fbbf24;
            text-align: right;
          }
        }
        .proc-status {
          width: 70px;
          font-size: 11px;
          font-weight: 500;
          text-align: center;
          padding: 4px 10px;
          border-radius: 20px;
          &.high {
            background: rgba(239,68,68,0.2);
            color: #f87171;
          }
          &.medium {
            background: rgba(245,158,11,0.2);
            color: #fbbf24;
          }
          &.low {
            background: rgba(16,185,129,0.2);
            color: #34d399;
          }
        }
      }
    }
    .process-summary {
      display: flex;
      justify-content: space-around;
      margin-top: 12px;
      padding-top: 10px;
      border-top: 1px solid rgba(59,130,246,0.15);
      .summary-item {
        text-align: center;
        span {
          display: block;
          font-size: 10px;
          color: #94a3b8;
          margin-bottom: 4px;
        }
        strong {
          font-size: 14px;
          font-weight: 700;
          color: #e2e8f0;
          &.warning {
            color: #f87171;
          }
        }
      }
    }
  }
}

.events-panel {
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 16px;
  
  .events-stats { display: flex; gap: 16px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid rgba(59,130,246,0.15);
    .stat-item { display: flex; flex-direction: column; span { font-size: 10px; color: #94a3b8; } strong { font-size: 18px; font-weight: 700; color: #3b82f6; &.warning { color: #f87171; } } }
  }
  
  .event-list { max-height: 280px; overflow-y: auto; &::-webkit-scrollbar { width: 4px; }
    .event-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid rgba(71,85,105,0.08);
      .event-time { font-family: monospace; font-size: 10px; color: #60a5fa; min-width: 55px; }
      .event-badge { font-size: 9px; padding: 2px 8px; border-radius: 12px; width: 44px; text-align: center; &.build { background: rgba(139,92,246,0.2); color: #a78bfa; } &.release { background: rgba(245,158,11,0.2); color: #fbbf24; } &.login { background: rgba(16,185,129,0.2); color: #34d399; } &.exec { background: rgba(59,130,246,0.2); color: #60a5fa; } &.warning { background: rgba(239,68,68,0.2); color: #f87171; } }
      .event-message { flex: 1; font-size: 11px; color: #cbd5e6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .event-machine { font-size: 9px; color: #64748b; background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 10px; }
    }
  }
  
  .alert-summary { margin-top: 16px; padding-top: 12px; border-top: 1px solid rgba(59,130,246,0.15);
    .alert-header { display: flex; justify-content: space-between; margin-bottom: 10px; span { font-size: 12px; font-weight: 500; color: #fbbf24; } .alert-count { background: rgba(239,68,68,0.2); color: #f87171; padding: 2px 8px; border-radius: 20px; font-size: 10px; } }
    .alert-list { .alert-item { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid rgba(71,85,105,0.05); .alert-content { flex: 1; .alert-title { font-size: 11px; font-weight: 500; color: #e2e8f0; } .alert-desc { font-size: 9px; color: #94a3b8; } } .alert-time { font-size: 9px; color: #64748b; } } .empty-alerts { text-align: center; padding: 15px; color: #64748b; font-size: 11px; } }
  }
  
  .quick-actions { margin-top: 16px; padding-top: 12px; border-top: 1px solid rgba(59,130,246,0.15);
    .quick-title { font-size: 11px; font-weight: 600; color: #94a3b8; margin-bottom: 10px; }
    .actions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px;
      .action-chip { display: flex; align-items: center; gap: 8px; padding: 8px; background: rgba(0,0,0,0.3); border-radius: 12px; cursor: pointer; transition: all 0.2s; &:hover { background: rgba(59,130,246,0.2); transform: translateX(2px); } .action-icon { font-size: 14px; } span { font-size: 11px; color: #e2e8f0; } }
    }
  }
}

.bottom-actions {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 12px 20px;
  
  .action-block {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    background: rgba(0,0,0,0.3);
    border-radius: 14px;
    cursor: pointer;
    transition: all 0.2s;
    &:hover { background: rgba(59,130,246,0.2); transform: translateY(-2px); .action-icon { transform: scale(1.05); } }
    .action-icon { width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center; i { font-size: 16px; color: white; } }
    .action-name { font-size: 11px; font-weight: 500; color: #e2e8f0; }
  }
}

@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

@media (max-width: 1400px) {
  .dashboard-main { grid-template-columns: 280px 1fr 320px; }
  .bottom-actions { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 1200px) {
  .dashboard-main { grid-template-columns: 1fr; }
  .bottom-actions { grid-template-columns: repeat(4, 1fr); }
}
</style>