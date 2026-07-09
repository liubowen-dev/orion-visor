<!-- src/components/command/HealthGauge.vue -->
<template>
  <div class="health-gauge">
    <div class="gauge-title">
      <a-icon type="heart" />
      系统健康度
    </div>
    <div class="gauge-container">
      <canvas ref="canvas" :width="200" :height="120"></canvas>
      <div class="gauge-value">
        <span class="value">{{ healthScore }}</span>
        <span class="unit">%</span>
      </div>
    </div>
    <div class="gauge-status" :class="statusClass">
      <a-icon :type="statusIcon" />
      <span>{{ statusText }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HealthGauge',
  props: {
    score: {
      type: Number,
      default: 98.5
    }
  },
  data() {
    return {
      healthScore: this.score
    }
  },
  computed: {
    statusClass() {
      if (this.healthScore >= 95) return 'status-excellent'
      if (this.healthScore >= 80) return 'status-good'
      if (this.healthScore >= 60) return 'status-warning'
      return 'status-critical'
    },
    statusText() {
      if (this.healthScore >= 95) return '优秀'
      if (this.healthScore >= 80) return '良好'
      if (this.healthScore >= 60) return '警告'
      return '危险'
    },
    statusIcon() {
      if (this.healthScore >= 95) return 'smile'
      if (this.healthScore >= 80) return 'meh'
      if (this.healthScore >= 60) return 'frown'
      return 'frown'
    }
  },
  watch: {
    score: {
      immediate: true,
      handler(val) {
        this.healthScore = val
        this.$nextTick(() => this.drawGauge())
      }
    }
  },
  mounted() {
    this.drawGauge()
  },
  methods: {
    drawGauge() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      
      const ctx = canvas.getContext('2d')
      const width = canvas.width
      const height = canvas.height
      
      ctx.clearRect(0, 0, width, height)
      
      const startAngle = Math.PI
      const endAngle = Math.PI * 2
      const radius = 80
      const centerX = width / 2
      const centerY = height - 10
      
      // 背景
      ctx.beginPath()
      ctx.arc(centerX, centerY, radius, startAngle, endAngle)
      ctx.strokeStyle = '#E2E8F0'
      ctx.lineWidth = 12
      ctx.stroke()
      
      // 前景
      const percent = this.healthScore / 100
      const angle = startAngle + (endAngle - startAngle) * percent
      
      ctx.beginPath()
      ctx.arc(centerX, centerY, radius, startAngle, angle)
      
      let color
      if (this.healthScore >= 95) color = '#10B981'
      else if (this.healthScore >= 80) color = '#3B82F6'
      else if (this.healthScore >= 60) color = '#F59E0B'
      else color = '#EF4444'
      
      ctx.strokeStyle = color
      ctx.lineWidth = 12
      ctx.stroke()
    }
  }
}
</script>

<style lang="less" scoped>
.health-gauge {
  background: white;
  border-radius: 20px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.02);
  
  .gauge-title {
    font-size: 14px;
    font-weight: 500;
    color: #64748B;
    margin-bottom: 12px;
    
    i {
      margin-right: 6px;
      color: #EF4444;
    }
  }
  
  .gauge-container {
    position: relative;
    display: flex;
    justify-content: center;
    
    canvas {
      width: 100%;
      height: auto;
    }
    
    .gauge-value {
      position: absolute;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      
      .value {
        font-size: 32px;
        font-weight: 700;
        color: #0F172A;
        font-family: monospace;
      }
      
      .unit {
        font-size: 14px;
        color: #64748B;
        margin-left: 2px;
      }
    }
  }
  
  .gauge-status {
    margin-top: 8px;
    font-size: 13px;
    font-weight: 500;
    
    i {
      margin-right: 4px;
    }
    
    &.status-excellent {
      color: #10B981;
    }
    
    &.status-good {
      color: #3B82F6;
    }
    
    &.status-warning {
      color: #F59E0B;
    }
    
    &.status-critical {
      color: #EF4444;
    }
  }
}
</style>