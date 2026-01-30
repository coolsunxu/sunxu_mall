import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录',
      hidden: true,
    },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: {
          title: '首页',
          icon: 'dashboard',
        },
      },
      {
        path: '/system/user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: {
          title: '用户管理',
          icon: 'user',
        },
      },
      {
        path: '/mall/product',
        name: 'Product',
        component: () => import('@/views/mall/product/index.vue'),
        meta: {
          title: '商品管理',
          icon: 'goods',
        },
      },
      {
        path: '/attributeValue',
        name: 'AttributeValue',
        component: () => import('@/views/mall/attributeValue/index.vue'),
        meta: {
          title: '属性值管理',
          icon: 'setting',
          hidden: true, // 方案一：隐藏独立入口，通过属性列表管理
        },
      },
      {
        path: '/attribute',
        name: 'Attribute',
        component: () => import('@/views/mall/attribute/index.vue'),
        meta: {
          title: '属性列表',
          icon: 'list',
        },
      },
    ],
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: {
      title: '404',
      hidden: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 路由守卫 - 检查登录状态
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  
  // 如果是登录页面，直接放行
  if (to.path === '/login') {
    // 如果已登录，重定向到首页
    if (token) {
      next('/')
    } else {
      next()
    }
    return
  }
  
  // 白名单页面（不需要登录）
  const whiteList = ['/login', '/404']
  if (whiteList.includes(to.path)) {
    next()
    return
  }
  
  // 需要登录的页面
  if (!token) {
    // 未登录，重定向到登录页
    next('/login')
    return
  }
  
  // 已登录，放行
  next()
})

export default router