import { useEffect } from 'react'
import { useHorizontalMode } from '@/utils/hooks'
import { isTV } from '@/utils/device'
import PageContent from '@/components/PageContent'
import { setComponentId } from '@/core/common'
import { COMPONENT_IDS } from '@/config/constant'
import Vertical from './Vertical'
import Horizontal from './Horizontal'
import { navigations } from '@/navigation'
import settingState from '@/store/setting/state'


interface Props {
  componentId: string
}

export default ({ componentId }: Props) => {
  const isHorizontalByLayout = useHorizontalMode()
  // TV 上强制横屏布局(Horizontal),因为它本来就是为横屏大屏设计的
  const isHorizontalMode = isHorizontalByLayout || isTV
  useEffect(() => {
    setComponentId(COMPONENT_IDS.home, componentId)
    // eslint-disable-next-line react-hooks/exhaustive-deps

    if (settingState.setting['player.startupPushPlayDetailScreen']) {
      navigations.pushPlayDetailScreen(componentId, true)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <PageContent>
      {
        isHorizontalMode
          ? <Horizontal />
          : <Vertical />
      }
    </PageContent>
  )
}
