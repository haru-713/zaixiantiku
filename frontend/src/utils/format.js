/**
 * 格式化日期时间
 * @param {string|number|Date} date 
 * @param {string} format 
 */
export function formatDateTime(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '-'
  const d = new Date(date)
  
  const o = {
    'M+': d.getMonth() + 1,
    'D+': d.getDate(),
    'H+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds()
  }

  let res = format.replace(/Y+/, (match) => {
    return (d.getFullYear() + '').substring(4 - match.length)
  })

  for (let k in o) {
    res = res.replace(new RegExp(k), (match) => {
      return match.length === 1 ? o[k] : ('00' + o[k]).substring(('' + o[k]).length)
    })
  }

  return res
}

/**
 * 格式化日期
 */
export function formatDate(date) {
  return formatDateTime(date, 'YYYY-MM-DD')
}
