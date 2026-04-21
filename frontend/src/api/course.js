import request from '@/utils/request'

/**
 * 课程相关接口
 */
export const courseApi = {
  /**
   * 分页获取课程列表
   */
  getCoursePage(params) {
    return request.get('/courses', { params })
  },

  /**
   * 获取课程详情
   */
  getCourseDetail(courseId) {
    return request.get(`/courses/${courseId}`)
  },

  /**
   * 创建课程
   */
  createCourse(data) {
    return request.post('/courses', data)
  },

  /**
   * 更新课程
   */
  updateCourse(courseId, data) {
    return request.put(`/courses/${courseId}`, data)
  },

  /**
   * 删除课程
   */
  deleteCourse(courseId) {
    return request.delete(`/courses/${courseId}`)
  }
}
