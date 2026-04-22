import request from '@/utils/request'

/**
 * 用户相关接口
 */
export const userApi = {
  /**
   * 登录
   */
  login(data) {
    return request.post('/auth/login', data)
  },

  /**
   * 注册
   */
  register(data) {
    return request.post('/auth/register', data)
  },

  /**
   * 获取当前用户信息
   */
  getProfile() {
    return request.get('/user/profile')
  },

  /**
   * 更新用户信息
   */
  updateProfile(data) {
    return request.put('/user/profile', data)
  },

  /**
   * 修改密码
   */
  updatePassword(data) {
    return request.put('/user/password', data)
  }
}

/**
 * 管理员：用户管理接口
 */
export const adminUserApi = {
  /**
   * 分页获取用户列表
   */
  getUserList(params) {
    return request.get('/admin/users', { params })
  },

  /**
   * 切换用户状态
   */
  toggleStatus(userId, status) {
    return request.put(`/admin/users/${userId}/status`, { status })
  },

  /**
   * 审核用户
   */
  auditUser(userId, data) {
    return request.put(`/admin/users/${userId}/audit`, data)
  },

  /**
   * 重置密码
   */
  resetPassword(userId) {
    return request.put(`/admin/users/${userId}/password/reset`)
  },

  /**
   * 删除用户
   */
  deleteUser(userId) {
    return request.delete(`/admin/users/${userId}`)
  }
}
