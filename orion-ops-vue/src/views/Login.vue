<template>
  <div class="login-container">
    <!-- 视频背景 -->
    <video 
      ref="videoBg"
      class="video-bg"
      autoplay
      loop
      muted
      playsinline
    >
      <source :src="bgVideo" type="video/mp4" />
    </video>
    
    <!-- 深色遮罩层 -->
    <div class="overlay"></div>
    
    <!-- 径向渐变光晕 -->
    <div class="radial-glow"></div>
    
    <!-- 登录表单 -->
    <div class="login-form-container">
      <div class="form-content">
        <!-- Logo 区域 - 简洁图标 -->
        <div class="logo-area">
          <div class="logo-ring">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="24" cy="24" r="22" stroke="url(#gradStroke)" stroke-width="2" fill="none"/>
              <circle cx="24" cy="24" r="6" fill="url(#gradFill)" />
              <defs>
                <linearGradient id="gradStroke" x1="0" y1="0" x2="48" y2="48">
                  <stop stop-color="#3B82F6"/>
                  <stop offset="1" stop-color="#06B6D4"/>
                </linearGradient>
                <linearGradient id="gradFill" x1="0" y1="0" x2="48" y2="48">
                  <stop stop-color="#3B82F6"/>
                  <stop offset="1" stop-color="#06B6D4"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
        </div>
        
        <!-- 标题 -->
        <h1 class="title">慧维智能运维管控系统</h1>
        <!-- <p class="subtitle">自动化运维平台</p> -->
        
        <!-- 登录表单 -->
        <a-form-model 
          class="login-form"
          :model="form"
          :rules="rules"
          ref="form"
          @submit="handleLoginSubmit"
        >
          <a-form-model-item prop="username">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </span>
              <input 
                type="text"
                placeholder="用户名"
                v-model="form.username"
                class="modern-input"
              />
            </div>
          </a-form-model-item>
          
          <a-form-model-item prop="password">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="5" y="11" width="14" height="11" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M8 11V7a4 4 0 0 1 8 0v4" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </span>
              <input 
                :type="showPassword ? 'text' : 'password'"
                placeholder="密码"
                v-model="form.password"
                class="modern-input"
              />
              <span class="password-toggle" @click="showPassword = !showPassword">
                <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="1.5"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M2 2L22 22" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M6.71 7.71A9.93 9.93 0 0 0 5 12s4 8 11 8c1.46 0 2.85-.33 4.12-.88" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M16.42 13.42A3 3 0 0 0 13 10" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M18.29 10.71A9.93 9.93 0 0 1 19 12s-4 8-11 8c-.74 0-1.46-.1-2.15-.29" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </span>
            </div>
          </a-form-model-item>
          
          <button 
            class="login-button"
            :disabled="isSubmit"
            @click="handleLoginSubmit"
          >
            <span v-if="!isSubmit">登 录</span>
            <span v-else class="loading-spinner"></span>
          </button>
        </a-form-model>
      </div>
    </div>
  </div>
</template>

<script>
import { md5, strToBoo } from '@/lib/utils'

export default {
  data() {
    return {
      demoMode: strToBoo(process.env.VUE_APP_DEMO_MODE),
      isSubmit: false,
      showPassword: false,
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名' }],
        password: [{ required: true, message: '请输入密码' }]
      },
      bgVideo: require('@/assets/科技2.mp4')
    }
  },
  mounted() {
    if (this.$refs.videoBg) {
      this.$refs.videoBg.play().catch(e => console.log('Video autoplay failed:', e))
    }
  },
  methods: {
    async handleLoginSubmit(e) {
      e.preventDefault()
      if (!this.form.username.trim() || !this.form.password.trim()) {
        return
      }
      this.isSubmit = true
      
      this.$api.login({
        username: this.form.username.trim(),
        password: md5(this.form.password.trim())
      }).then(({ data }) => {
        const user = {
          userId: data.userId,
          username: data.username,
          nickname: data.nickname,
          roleType: data.roleType
        }
        this.$storage.set(this.$storage.keys.LOGIN_TOKEN, data.token)
        this.$storage.set(this.$storage.keys.CURRENT_USER, JSON.stringify(user))
        this.$router.push({ path: '/console-full' })
      }).catch(() => {
        this.isSubmit = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

// 视频背景
.video-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  min-width: 100%;
  min-height: 100%;
  width: auto;
  height: auto;
  transform: translate(-50%, -50%);
  object-fit: cover;
  z-index: 0;
}

// 深色遮罩
.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1;
}

// 径向光晕
.radial-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 80%;
  height: 80%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15), transparent 70%);
  z-index: 1;
  pointer-events: none;
}

// 登录表单容器
.login-form-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 380px;
  z-index: 10;
}

.form-content {
  text-align: center;
}

// Logo 区域
.logo-area {
  margin-bottom: 32px;
  
  .logo-ring {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    animation: logoPulse 2s ease-in-out infinite;
    
    svg {
      filter: drop-shadow(0 0 15px rgba(59, 130, 246, 0.5));
    }
  }
}

@keyframes logoPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

// 标题
.title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #FFFFFF, #94A3F8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: 'Inter', sans-serif;
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
  font-weight: 400;
  letter-spacing: 0.5px;
}

// 表单
.login-form {
  ::v-deep .ant-form-item {
    margin-bottom: 20px;
  }
}

// 输入框包装器
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  
  .input-icon {
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    color: rgba(255, 255, 255, 0.5);
    transition: color 0.3s;
    z-index: 1;
    pointer-events: none;
  }
  
  .modern-input {
    width: 100%;
    height: 50px;
    padding: 0 45px;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 25px;
    color: #FFFFFF;
    font-size: 14px;
    font-weight: 400;
    transition: all 0.3s;
    font-family: 'Inter', sans-serif;
    
    &::placeholder {
      color: rgba(255, 255, 255, 0.4);
      font-weight: 400;
    }
    
    &:focus {
      outline: none;
      background: rgba(255, 255, 255, 0.12);
      border-color: #3B82F6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
    }
    
    &:focus + .input-icon {
      color: #3B82F6;
    }
  }
  
  .password-toggle {
    position: absolute;
    right: 16px;
    top: 50%;
    transform: translateY(-50%);
    color: rgba(255, 255, 255, 0.5);
    cursor: pointer;
    transition: color 0.3s;
    z-index: 1;
    
    &:hover {
      color: #3B82F6;
    }
  }
  
  &:hover .input-icon {
    color: rgba(255, 255, 255, 0.7);
  }
}

// 登录按钮
.login-button {
  width: 100%;
  height: 50px;
  margin-top: 8px;
  background: linear-gradient(135deg, #3B82F6, #06B6D4);
  border: none;
  border-radius: 25px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-family: 'Inter', sans-serif;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
  
  .loading-spinner {
    width: 20px;
    height: 20px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    display: inline-block;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>