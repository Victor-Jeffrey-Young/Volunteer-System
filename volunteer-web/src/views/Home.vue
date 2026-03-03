<template>
  <div>
    <div class="stat-cards">
      <div class="card card-1">
        <h4>总志愿者人数</h4>
        <p class="num">1,280 人</p>
      </div>
      <div class="card card-2">
        <h4>累计志愿时长</h4>
        <p class="num">4,560 小时</p>
      </div>
      <div class="card card-3">
        <h4>进行中活动</h4>
        <p class="num">12 个</p>
      </div>
    </div>

    <div style="margin-top: 30px; background: white; padding: 20px; border-radius: 8px;">
      <h3>系统公告</h3>
      <ul>
        <li>欢迎进入社区志愿服务管理系统开发测试版。</li>
        <li>当前模块：用户管理、认证模块已完成。</li>
        <li>待开发：活动管理、报名管理、数据可视化看板。</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.card { padding: 20px; border-radius: 8px; color: white; }
.card-1 { background: #52c41a; }
.card-2 { background: #1890ff; }
.card-3 { background: #722ed1; }
.num { font-size: 24px; font-weight: bold; margin-top: 10px; }
</style>