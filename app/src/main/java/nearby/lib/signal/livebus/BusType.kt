package nearby.lib.signal.livebus

/***
 * BusType指令标识
 */
object BusType {

    /***
     * 网络接口提示语
     */
    const val BUS_NET_MSG = "netMsg"
    /***
     * 倒计时
     */
    const val BUS_RESET_COUNTDOWN = "resetCountdown"

    /***
     * 计重页
     */
    const val BUS_DELIVERY_STATUS = "deliveryStatus"
    /***
     * 计重页关闭
     */
    const val BUS_DELIVERY_CLOSE = "deliveryClose"

    /***
     * 计重页格口异常
     */
    const val BUS_DELIVERY_ABNORMAL = "deliveryAbnormal"
    /***
     * 关闭手机页面
     */
    const val BUS_MOBILE_CLOS = "mobileClose"
    /***
     * 图片
     */
    const val BUS_DELIVERY_PHOTO = "deliveryPhoto"
    /***
     * 投口1状态类型
     */
    const val BUS_TOU1_DOOR_STATUS = "tou1DoorStatus"

    /***
     * 投口2状态类型
     */
    const val BUS_TOU2_DOOR_STATUS = "tou2DoorStatus"

    /***
     * 满溢
     */
    const val BUS_OVERFLOW = "overflow"

    /**
     * 故障
     */
    const val BUS_FAULT = "fault"

    /**
     * 正常
     */
    const val BUS_NORMAL = "normal"

    /**
     * 刷新数据
     */
    const val BUS_REFRESH_DATA = "data"
}