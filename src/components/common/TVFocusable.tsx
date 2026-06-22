/**
 * TV 适配:把任意触摸组件包成支持 D-pad focus 的版本。
 *
 * 用法:
 *   <TVFocusable onPress={...}>
 *     <TouchableOpacity>...</TouchableOpacity>
 *   </TVFocusable>
 *
 * 或者直接用它替代 TouchableOpacity:
 *   <TVFocusable onPress={...}>
 *     <View>...</View>
 *   </TVFocusable>
 *
 * 工作原理:
 * - 外层 View 设 focusable=true + nextFocus* 让 Android focus 系统能命中
 * - 用 onFocus / onBlur 维护视觉状态
 * - 移动端完全无副作用(走普通 Press 路径)
 */
import { useRef, useState, useCallback } from 'react'
import { View, Platform } from 'react-native'
import { isTV } from '@/utils/device'

export interface TVFocusableProps {
  children: React.ReactNode
  style?: any
  onPress?: () => void
  // 用于 Android TV focus chain
  nextFocusDown?: number
  nextFocusForward?: number
  nextFocusLeft?: number
  nextFocusRight?: number
  nextFocusUp?: number
  hasTVPreferredFocus?: boolean
}

export default ({
  children,
  style,
  onPress,
  nextFocusDown,
  nextFocusForward,
  nextFocusLeft,
  nextFocusRight,
  nextFocusUp,
  hasTVPreferredFocus,
}: TVFocusableProps) => {
  const [focused, setFocused] = useState(false)
  const ref = useRef<View>(null)

  // 仅在 TV 模式下注入 focus 行为,避免影响移动端
  if (!isTV) {
    return <View onTouchEnd={onPress}>{children}</View>
  }

  return (
    <View
      ref={ref}
      focusable={true}
      // 直接传到底层 Android View(ReactViewManager 已经处理)
      // @ts-ignore: 这些 prop 在 RN 0.73 types 里没标
      nextFocusDown={nextFocusDown}
      nextFocusForward={nextFocusForward}
      nextFocusLeft={nextFocusLeft}
      nextFocusRight={nextFocusRight}
      nextFocusUp={nextFocusUp}
      hasTVPreferredFocus={hasTVPreferredFocus}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      onTouchEnd={onPress}
      style={[
        style,
        focused && {
          borderWidth: 3,
          borderColor: '#FFEB3B',
          backgroundColor: 'rgba(255, 235, 59, 0.1)',
          transform: [{ scale: 1.02 }],
        },
      ]}
    >
      {children}
    </View>
  )
}
