/**
 * TV 适配:全局 D-pad key event listener + spatial focus algorithm.
 *
 * 工作原理:
 * - 监听 Java 层 emit 的 "TV_DPAD_EVENT" 事件
 * - 维护一组 registerFocusable(ref, viewId, layout) 的可 focus 元素
 * - D-pad 事件来时,根据几何位置算最近邻
 * - 找到后调用 ref.current?.measure / 实际显示高亮(用 state)
 */
import { useEffect, useRef, useCallback, useState } from 'react'
import { DeviceEventEmitter, findNodeHandle, UIManager, View } from 'react-native'
import { isTV } from '@/utils/device'

type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT' | 'CENTER' | 'BACK' | 'MENU'

interface FocusableEntry {
  id: string
  ref: React.RefObject<View>
  onPress?: () => void
}

let focusables: FocusableEntry[] = []
let currentFocusedId: string | null = null
let listeners: Array<(focusedId: string | null) => void> = []

export function getCurrentFocusedId(): string | null {
  return currentFocusedId
}

export function subscribeFocus(listener: (id: string | null) => void): () => void {
  listeners.push(listener)
  return () => {
    listeners = listeners.filter(l => l !== listener)
  }
}

function notifyListeners() {
  for (const l of listeners) l(currentFocusedId)
}

/**
 * 注册一个 focusable 元素(自动收集 measure 信息)
 * 返回 isFocused 状态让组件显示高亮
 */
export function useTVFocusable(id: string, onPress?: () => void) {
  const ref = useRef<View>(null)
  const [isFocused, setIsFocused] = useState(false)

  useEffect(() => {
    if (!isTV) return
    const entry: FocusableEntry = { id, ref, onPress }
    focusables.push(entry)
    // 默认第一个 focusable 拿焦点
    if (focusables.length === 1 && !currentFocusedId) {
      currentFocusedId = id
      setIsFocused(true)
      notifyListeners()
    }
    const unsub = subscribeFocus(fid => {
      const focused = fid === id
      setIsFocused(focused)
    })
    return () => {
      focusables = focusables.filter(e => e.id !== id)
      if (currentFocusedId === id) {
        currentFocusedId = focusables[0]?.id || null
        setIsFocused(false)
        notifyListeners()
      }
      unsub()
    }
  }, [id])

  return { ref, isFocused }
}

function computeMeasure(entry: FocusableEntry): Promise<{ x: number, y: number, w: number, h: number } | null> {
  return new Promise(resolve => {
    if (!entry.ref.current) return resolve(null)
    entry.ref.current.measureInWindow((x, y, w, h) => resolve({ x, y, w, h }))
  })
}

async function moveFocus(direction: 'UP' | 'DOWN' | 'LEFT' | 'RIGHT') {
  if (focusables.length === 0) return
  const current = focusables.find(e => e.id === currentFocusedId) || focusables[0]
  const currentMeas = await computeMeasure(current)
  if (!currentMeas) return

  let best: { entry: FocusableEntry, score: number } | null = null
  for (const e of focusables) {
    if (e.id === currentFocusedId) continue
    const m = await computeMeasure(e)
    if (!m) continue

    const curCenter = { x: currentMeas.x + currentMeas.w / 2, y: currentMeas.y + currentMeas.h / 2 }
    const newCenter = { x: m.x + m.w / 2, y: m.y + m.h / 2 }
    const dx = newCenter.x - curCenter.x
    const dy = newCenter.y - curCenter.y

    let primary = 0
    let secondary = 0
    if (direction === 'UP') { primary = -dy; secondary = Math.abs(dx) }
    else if (direction === 'DOWN') { primary = dy; secondary = Math.abs(dx) }
    else if (direction === 'LEFT') { primary = -dx; secondary = Math.abs(dy) }
    else if (direction === 'RIGHT') { primary = dx; secondary = Math.abs(dy) }

    if (primary <= 0) continue

    const dist = Math.sqrt(dx * dx + dy * dy)
    // 加权得分:主轴距离 + 次轴距离 * 2
    const score = primary + secondary * 2

    if (!best || score < best.score) {
      best = { entry: e, score }
    }
  }

  if (best) {
    currentFocusedId = best.entry.id
    notifyListeners()
  }
}

function activateCurrent() {
  const cur = focusables.find(e => e.id === currentFocusedId)
  if (cur?.onPress) cur.onPress()
}

/**
 * 安装一次性的全局 TV D-pad 监听(JS 端入口)
 */
export function installTVNavigation() {
  if (!isTV) return
  const sub = DeviceEventEmitter.addListener('TV_DPAD_EVENT', (keyName: string) => {
    switch (keyName) {
      case 'DPAD_UP': void moveFocus('UP'); break
      case 'DPAD_DOWN': void moveFocus('DOWN'); break
      case 'DPAD_LEFT': void moveFocus('LEFT'); break
      case 'DPAD_RIGHT': void moveFocus('RIGHT'); break
      case 'DPAD_CENTER': activateCurrent(); break
      case 'BACK': break
      case 'MENU': break
    }
  })
  return () => sub.remove()
}
