import { RouteRecordRaw } from 'vue-router'
import { readdirSync, statSync } from 'fs'
import { join, extname, dirname } from 'path'

const viewsPath = join(__dirname, '../views')

function generateRoutes(dir: string = viewsPath, basePath: string = ''): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  const items = readdirSync(dir)

  for (const item of items) {
    const fullPath = join(dir, item)
    const stat = statSync(fullPath)

    if (stat.isDirectory()) {
      const subRoutes = generateRoutes(fullPath, join(basePath, item))
      routes.push(...subRoutes)
    } else if (extname(item) === '.vue') {
      const routePath = join(basePath, item.replace('.vue', ''))
      const componentPath = `@/views${routePath}.vue`

      routes.push({
        path: `/${routePath}`,
        name: routePath.replace(/\//g, '_'),
        component: () => import(componentPath)
      })
    }
  }

  return routes
}

export default generateRoutes