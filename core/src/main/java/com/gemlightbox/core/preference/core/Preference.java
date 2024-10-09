package com.gemlightbox.core.preference.core;

/**
 * Created by hanz on 10.05.2017.
 */

public abstract class Preference {
    private static final String MY_PREFERENCES = "CameraPickPreferences";
    public static final String TOKEN_KEY = "jwt_auth_token";
    public static final String API_TOKEN_KEY = "token";
    public static final String WORKMANAGER_ID = "WORKMANAGER_ID";
    public static final String SECRET = "secret";
    public static final String TOKEN_SKU = "sku";
    public static final String UNIQUE_ID = "unique_id";
    public static final String DEVICE_ID = "device_id";
    public static final String PAID_TYPE = "paid_type";
    public static final String EMAIL_KEY = "email_key";
    public static final String TEMP_EMAIL_KEY = "temp_email_key";
    public static final String CHECK_SYNCED_ITEMS = "check_synced_items";
    public static final String PASSW_KEY = "password_key";
    public static final String FILES_CHECKED_KEY = "checked_key";
    public static final String UPLOADING_AVAILABLE_KEY = "uploading_available";
    public static final String MESSAGE = "message";
    public static final String SHOW_ECLIPSE_DIALOG = "show_eclipse_dialog1";
    public static final String SHOW_MACRO_DIALOG = "show_macro_dialog";
    public static final String SHOW_GEM_LOUPE_DIALOG = "show_gem_loupe_dialog";
    public static final String WHITE_BALANCE_ENABLE = "white_balance_enable";
    public static final String WHITE_BALANCE_TEMP = "white_balance_temp";
    public static final String WHITE_BALANCE_TINT = "white_balance_tint";
    public static final String WHITE_BALANCE_CAMERA_TEST = "white_balance_camera_test1";
    public static final String AUTO_CONNECT_BLUETOOTH = "auto_connect_bluetooth";
    public static final String SAVE_PHOTOS_IN_MAX_RESOLUTION = "save_in_max_resolution";
    public static final String SHOW_CAMERA_ALERT_DIALOG = "show_camera_alert_dialog";
    public static final String CAMERA_X_ENABLED = "camera_x_enabled";
    public static final String SAVE_VIDEOS_IN_4K = "save_videos_in_4k";
    public static final String PRO = "pro";
    public static final String NATIVE_CAMERA_KEY = "native_camera";
    public static final String FIRST_START_KEY = "first_start";
    public static final String ON_BOARD_COMPLETE = "ON_BOARD_COMPLETE";
    public static final String APP_VERSION_CODE = "APP_VERSION_CODE";
    public static final String TUTORIALS_FIRST_TIME = "tutorials_first_time";
    public static final String TUTORIALS_CAMERA_FIRST_TIME = "TUTORIALS_CAMERA_FIRST_TIME";
    public static final String LOGO_LIST = "logo_list";
    public static final String LOGO_INDEX = "logo_index";
    public static final String LOGO_ENABLE = "logo_enable";
    public static final String DEVICE_TO_AUTOCONNECT = "device_to_autoconnect";
    public static final String GALLERY_LAYOUT_MANAGER_TYPE = "gallery_layout_manager";
    public static final String CATALOGS_LAYOUT_MANAGER_TYPE = "catalogs_layout_manager";
    public static final String EDIT_SAVE_DIALOG_ENABLE = "EDIT_SAVE_DIALOG_ENABLE";
    public static final String EXTERNAL_CAMERA_WIDTH = "EXTERNAL_CAMERA_WIDTH";
    public static final String EXTERNAL_CAMERA_HEIGHT = "EXTERNAL_CAMERA_HEIGHT";
    public static final String EXTERNAL_ZOOM_LVL = "EXTERNAL_ZOOM_LVL";

    public static final String EDIT_IMAGE_PATH = "edit_image_path";
    public static final String LOGO_POSITION_CHANGED = "logo_position_changed";
    public static final String IS_DRAW_SKU = "is_draw_sku";
    public static final String IS_WATERMARK = "is_watermark";
    public static final String WATERMARK_TEXT = "watermark_text";
    public static final String SKU_STICKER = "sku_sticker";
    public static final String SKU_COLOR = "sku_color";
    public static final String SKU_X_COORDINATE = "sku_x_coordinate";
    public static final String SKU_Y_COORDINATE = "sku_y_coordinate";
    public static final String SKU_ALIGN = "sku_align";
    public static final String SKU_TYPEFACE = "sku_typeface";
    public static final String SKU_SIZE = "sku_size";
    public static final String SKU_CONTAINER_SIZE = "sku_container_size";
    public static final String WATERMARK_STICKER = "watermark_sticker";
    public static final String WATERMARK_COLOR = "watermark_color";
    public static final String WATERMARK_ALIGN = "watermark_align";
    public static final String WATERMARK_TYPEFACE = "watermark_typeface";
    public static final String WATERMARK_SIZE = "watermark_size";
    public static final String WATERMARK_OPACITY = "watermark_opacity";
    public static final String MANUAL_FOCUS = "MANUAL_FOCUS";
    public static final String ENABLE_ECLIPSE = "enable_eclipse_new";
    public static final String ENABLE_GEMLOUPE = "ENABLE_GEMLOUPE";
    public static final String ENABLE_RENDERSCRIPT = "enable_renderscript2";
    public static final String ONLINE_GALLERY_ENABLED = "online_gallery_enabled";
    public static final String SECRET_MODE = "enable_filter";
    public static final String WEB_LINK_TUTORIAL_SHOWN = "web_link_tutorial_shown";

    public static final String UPLOAD_WITHOUT_WIFI = "UPLOAD_WITHOUT_WIFI";
    public static final String ENHANCE_IMAGE = "ENHANCE_IMAGE";

    public static final String SUBSCRIBE_STATUS = "SUBSCRIBE_STATUS";
    public static final String IS_SUBACCOUNT = "IS_SUBACCOUNT";
    public static final String VERIFICATION_STATUS = "VERIFICATION_STATUS";
    public static final String BURST_ENABLED = "BURST_ENABLED";
    public static final String BURST_ENABLED_MACRO = "BURST_ENABLED_MACRO";
    public static final String BURST_MODEL_ENABED = "BURST_MODEL_ENABLED";
    public static final String BURST_REMOVE_BACKGROUND_IMAGES_ENABLED = "BURST_REMOVE_BACKGROUND_IMAGES_ENABLED";
    public static final String BURST_REMOVE_BACKGROUND_VIDEO_ENABLED = "BURST_REMOVE_BACKGROUND_VIDEO_ENABLED";
    public static final String BURST_AI_RETOUCH_IMAGES_ENABLED = "BURST_AI_RETOUCH_IMAGES_ENABLED";
    public static final String BURST_AI_DESC_ENABLED = "BURST_AI_DESC_ENABLED";
    public static final String USER_SYNCED = "USER_SYNCED";
    public static final String STORAGE_IS_FULL = "STORAGE_IS_FULL";
    public static final String SHOW_FULL_STORAGE_DIALOG = "SHOW_FULL_STORAGE_DIALOG";
    public static final String SHOW_ONLINE_GALLERY_SUBACCOUNT_DIALOG = "SHOW_ONLINE_GALLERY_SUBACCOUNT_DIALOG";
    public static final String STORAGE_MAX_SIZE = "STORAGE_MAX_SIZE_FLOAT";
    public static final String VERIFY_ITEM_SHOWN = "VERIFY_ITEM_SHOWN";
    public static final String FILES_RENAMED = "FILES_RENAMED";
    public static final String SYNC_FILES_COUNT = "SYNC_FILES_COUNT";
    public static final String SYNC_FILES_CHANGED = "SYNC_FILES_CHANGED";
    public static final String LOGGED_WITH_QR = "LOGGED_WITH_QR";
    public static final String BG_REMOVE_TIMES = "BG_REMOVE_TIMES";
    public static final String LAST_BG_REMOVE_TIME = "LAST_BG_REMOVE_TIME";

    public static final String ISO_KEY = "iso";
    public static final String ISO_PRO_KEY = "iso_pro";
    public static final String WHITE_BG_AUTO = "white_bg_auto";
    public static final String WHITE_BG_ECLIPSE = "white_bg_eclipse";
    public static final String WHITE_BG_MACRO = "white_bg_macro";
    public static final String WHITE_BG_GEMLOUPE = "white_bg_gemloupe";

    public static final String FILTER_SETTING = "filter_setting";
    public static final String FILTER_TONE_AUTO = "filter_tone_auto";
    public static final String FILTER_TONE_ECLIPSE = "filter_tone_eclipse";
    public static final String FILTER_TONE_MACRO = "filter_tone_macro";
    public static final String ZOOM_LVL = "zoom_lvl_v1";
    public static final String ZOOM_LVL_V2 = "zoom_lvl_v2";
    public static final String WHITE_BALANCE = "white_balance_enable";
    public static final String PHOTO_CAMERA_VIEW_ZOOM_LVL = "photo_camera_view_zoom_lvl";

    public static final String OVERLAY_VISIBILITY_SETTING = "overlay_visibility_setting";
    public static final String RATIO_SETTING = "ratio_setting";
    public static final String RATIO_TYPE = "ratio_type";
    public static final String RATIO_WIDTH = "ratio_width";
    public static final String RATIO_HEIGHT = "ratio_height";

    public static final String CONTRAST_PROGRESS = "contrast_progress";
    public static final String SHARPEN_PROGRESS = "sharpen_progress";

    public static final String SAFE_MODE_KEY = "safe_mode_new";
    public static final String CAMERA_STABILIZATION = "stabilize";


    public static final String CAMERA_SHOOT_TYPE = "CAMERA_SHOOT_TYPE";
    public static final String FULL_SIZE_PREVIEW = "full_size_preview_v2";

    public static final  String APP_UPDATE_LAST_CHECK_TIME = "app_update_last_check_time";
    public static final  String APP_UPDATE_LAST_VERSION = "app_update_last_version";
    public static final  String CLOUD_STORAGE_FULL_DIALOG_SHOW_TIME = "cloud_storage_full_dialog_show_time";


}
