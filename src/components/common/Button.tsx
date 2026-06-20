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
}


export interface BtnType {
  measure: (callback: (x: number, y: number, width: number, height: number, pageX: number, pageY: number) => void) => void
}

export default forwardRef<BtnType, BtnProps>(({ ripple: propsRipple = {}, disabled, children, style, ...props }, ref) => {
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

  // TV 焦点高亮:在 Pressable 的 state.style 里加 focused/pressed 反馈。
  // - 非 TV:跟以前一样,只控制 opacity
  // - TV:focused 时套主题 hover 色背景 + 边框 + 轻微缩放,给遥控器方向键视觉反馈
  const composedStyle = (state: PressableStateCallbackType): any => {
    const baseStyle = { opacity: disabled ? 0.3 : 1 }
    const isActive = !disabled && (state.focused || state.pressed)
    if (isTV && isActive) {
      return [
        baseStyle,
        {
          backgroundColor: theme['c-button-background-hover'],
          borderColor: theme['c-primary-font-hover'],
          borderWidth: 2,
          transform: [{ scale: 1.03 }],
        },
        typeof style === 'function' ? style(state) : style,
      ]
    }
    return [baseStyle, typeof style === 'function' ? style(state) : style]
  }

  return (
    <Pressable
      android_ripple={ripple}
      disabled={disabled}
      style={composedStyle}
      hasTVPreferredFocus={isTV && props.hasTVPreferredFocus}
      {...props}
      ref={btnRef}
    >
      {children}
    </Pressable>
  )
})
