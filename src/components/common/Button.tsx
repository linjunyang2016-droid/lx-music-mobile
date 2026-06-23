import React from 'react'
import { useTheme } from '@/store/theme/hook'
import { useMemo, useRef, useImperativeHandle, forwardRef } from 'react'
import { Pressable, type PressableProps, type PressableStateCallbackType, StyleSheet, type View, type ViewProps } from 'react-native'
import { isTV } from '@/utils/device'
// import { AppColors } from '@/theme'


export interface BtnProps extends PressableProps {
  ripple?: PressableProps['android_ripple']
  style?: ViewProps['style'] | PressableStateCallbackType['style']
  onChangeText?: (value: string) => void
  onClearText?: () => void
  children: React.ReactNode
  // TV 适配:显式传 nextFocus 链(可选,用户自己用 ref 算)
  nextFocusDown?: number
  nextFocusUp?: number
  nextFocusLeft?: number
  nextFocusRight?: number
}


export interface BtnType {
  measure: (callback: (x: number, y: number, width: number, height: number, pageX: number, pageY: number) => void) => void
}

export default forwardRef<BtnType, BtnProps>(({
  ripple: propsRipple = {},
  disabled,
  children,
  style,
  nextFocusDown,
  nextFocusUp,
  nextFocusLeft,
  nextFocusRight,
  ...props
}, ref) => {
  const theme = useTheme()
  const btnRef = useRef<View>(null)
  const ripple = useMemo(() => ({
    color: theme['c-primary-light-200-alpha-700'],
    ...propsRipple,
  }), [theme, propsRipple])

  useImperativeHandle(ref, () => ({
    measure(callback) {
      btnRef.current?.measure(callback)
    },
  }))

  // TV 焦点高亮:state.focused 在 TV 模式下是真正的 D-pad focus 状态
  // (MainActivity 已经在 onResume 时把所有 RN View 设成 focusableInTouchMode=true)
  const composedStyle = (state: PressableStateCallbackType): any => {
    const baseStyle = { opacity: disabled ? 0.3 : 1 }
    const isActive = !disabled && (state.focused || state.pressed)
    if (isTV && isActive) {
      return [
        baseStyle,
        {
          backgroundColor: '#FFEB3B',          // 纯黄色 — TV 上最显眼的对比
          borderColor: '#FF5722',             // 橙红色边框
          borderWidth: 5,                     // 5px 粗边框,远距离可见
          borderRadius: 4,
          transform: [{ scale: 1.05 }],       // 5% 放大
        },
        typeof style === 'function' ? style(state) : style,
      ]
    }
    return [baseStyle, typeof style === 'function' ? style(state) : style]
  }

  // TV 适配:在 TV 模式下,自动注册到全局 focus 列表
  const { useTVFocusable } = require('@/utils/tvNavigation')
  const tvFocus = isTV ? useTVFocusable(
    React.useId ? React.useId() : `btn-${Math.random().toString(36).slice(2)}`,
    props.onPress
  ) : { ref: btnRef, isFocused: false }

  return (
    <Pressable
      android_ripple={ripple}
      disabled={disabled}
      style={(state) => {
        const baseStyle = composedStyle(state)
        // TV 模式 + 焦点状态:加强高亮
        if (isTV && tvFocus.isFocused) {
          return [
            baseStyle,
            {
              backgroundColor: '#FFEB3B',
              borderColor: '#FF5722',
              borderWidth: 5,
              borderRadius: 4,
            },
          ]
        }
        return baseStyle
      }}
      hasTVPreferredFocus={isTV && props.hasTVPreferredFocus}
      // @ts-ignore: 这些 prop 在 RN 0.73 types 里没标
      nextFocusDown={isTV ? nextFocusDown : undefined}
      nextFocusUp={isTV ? nextFocusUp : undefined}
      nextFocusLeft={isTV ? nextFocusLeft : undefined}
      nextFocusRight={isTV ? nextFocusRight : undefined}
      {...props}
      ref={isTV ? tvFocus.ref : btnRef}
    >
      {children}
    </Pressable>
  )
})