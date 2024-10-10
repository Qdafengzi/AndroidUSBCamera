package com.camera.demo

class UsbConstants {
    companion object {
        const val USB_DIR_OUT = 0
        const val USB_DIR_IN = 0x80
        const val USB_TYPE_MASK = (0x03 shl 5)
        const val USB_TYPE_STANDARD = (0x00 shl 5)
        const val USB_TYPE_CLASS = (0x01 shl 5)
        const val USB_TYPE_VENDOR = (0x02 shl 5)
        const val USB_TYPE_RESERVED = (0x03 shl 5)
        const val USB_RECIP_MASK = 0x1f
        const val USB_RECIP_DEVICE = 0x00
        const val USB_RECIP_INTERFACE = 0x01
        const val USB_RECIP_ENDPOINT = 0x02
        const val USB_RECIP_OTHER = 0x03
        const val USB_RECIP_PORT = 0x04
        const val USB_RECIP_RPIPE = 0x05
        const val USB_REQ_GET_STATUS = 0x00
        const val USB_REQ_CLEAR_FEATURE = 0x01
        const val USB_REQ_SET_FEATURE = 0x03
        const val USB_REQ_SET_ADDRESS = 0x05
        const val USB_REQ_GET_DESCRIPTOR = 0x06
        const val USB_REQ_SET_DESCRIPTOR = 0x07
        const val USB_REQ_GET_CONFIGURATION = 0x08
        const val USB_REQ_SET_CONFIGURATION = 0x09
        const val USB_REQ_GET_INTERFACE = 0x0A
        const val USB_REQ_SET_INTERFACE = 0x0B
        const val USB_REQ_SYNCH_FRAME = 0x0C
        const val USB_REQ_SET_SEL = 0x30
        const val USB_REQ_SET_ISOCH_DELAY = 0x31
        const val USB_REQ_SET_ENCRYPTION = 0x0D
        const val USB_REQ_GET_ENCRYPTION = 0x0E
        const val USB_REQ_RPIPE_ABORT = 0x0E
        const val USB_REQ_SET_HANDSHAKE = 0x0F
        const val USB_REQ_RPIPE_RESET = 0x0F
        const val USB_REQ_GET_HANDSHAKE = 0x10
        const val USB_REQ_SET_CONNECTION = 0x11
        const val USB_REQ_SET_SECURITY_DATA = 0x12
        const val USB_REQ_GET_SECURITY_DATA = 0x13
        const val USB_REQ_SET_WUSB_DATA = 0x14
        const val USB_REQ_LOOPBACK_DATA_WRITE = 0x15
        const val USB_REQ_LOOPBACK_DATA_READ = 0x16
        const val USB_REQ_SET_INTERFACE_DS = 0x17

        const val USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT or USB_TYPE_STANDARD or USB_RECIP_DEVICE) // 0x10
        const val USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN or USB_TYPE_STANDARD or USB_RECIP_DEVICE) // 0x90
        const val USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT or USB_TYPE_STANDARD or USB_RECIP_INTERFACE) // 0x11
        const val USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN or USB_TYPE_STANDARD or USB_RECIP_INTERFACE) // 0x91
        const val USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT or USB_TYPE_STANDARD or USB_RECIP_ENDPOINT) // 0x12
        const val USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN or USB_TYPE_STANDARD or USB_RECIP_ENDPOINT) // 0x92

        const val USB_REQ_CS_DEVICE_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_DEVICE) // 0x20
        const val USB_REQ_CS_DEVICE_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_DEVICE) // 0xa0
        const val USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_INTERFACE) // 0x21
        const val USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_INTERFACE) // 0xa1
        const val USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_ENDPOINT) // 0x22
        const val USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_ENDPOINT) // 0xa2

        const val USB_REQ_VENDOR_DEVICE_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_DEVICE) // 0x40
        const val USB_REQ_VENDOR_DEVICE_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_DEVICE) // 0xc0
        const val USB_REQ_VENDOR_INTERFACE_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_INTERFACE) // 0x41
        const val USB_REQ_VENDOR_INTERFACE_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_INTERFACE) // 0xc1
        const val USB_REQ_VENDOR_ENDPOINT_SET = (USB_DIR_OUT or USB_TYPE_CLASS or USB_RECIP_ENDPOINT) // 0x42
        const val USB_REQ_VENDOR_ENDPOINT_GET = (USB_DIR_IN or USB_TYPE_CLASS or USB_RECIP_ENDPOINT) // 0xc2

        const val USB_DT_DEVICE = 0x01
        const val USB_DT_CONFIG = 0x02
        const val USB_DT_STRING = 0x03
        const val USB_DT_INTERFACE = 0x04
        const val USB_DT_ENDPOINT = 0x05
        const val USB_DT_DEVICE_QUALIFIER = 0x06
        const val USB_DT_OTHER_SPEED_CONFIG = 0x07
        const val USB_DT_INTERFACE_POWER = 0x08
        const val USB_DT_OTG = 0x09
        const val USB_DT_DEBUG = 0x0a
        const val USB_DT_INTERFACE_ASSOCIATION = 0x0b
        const val USB_DT_SECURITY = 0x0c
        const val USB_DT_KEY = 0x0d
        const val USB_DT_ENCRYPTION_TYPE = 0x0e
        const val USB_DT_BOS = 0x0f
        const val USB_DT_DEVICE_CAPABILITY = 0x10
        const val USB_DT_WIRELESS_ENDPOINT_COMP = 0x11
        const val USB_DT_WIRE_ADAPTER = 0x21
        const val USB_DT_RPIPE = 0x22
        const val USB_DT_CS_RADIO_CONTROL = 0x23
        const val USB_DT_PIPE_USAGE = 0x24
        const val USB_DT_SS_ENDPOINT_COMP = 0x30
        const val USB_DT_CS_DEVICE = (USB_TYPE_CLASS or USB_DT_DEVICE)
        const val USB_DT_CS_CONFIG = (USB_TYPE_CLASS or USB_DT_CONFIG)
        const val USB_DT_CS_STRING = (USB_TYPE_CLASS or USB_DT_STRING)
        const val USB_DT_CS_INTERFACE = (USB_TYPE_CLASS or USB_DT_INTERFACE)
        const val USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS or USB_DT_ENDPOINT)
        const val USB_DT_DEVICE_SIZE = 18
    }
}
