package com.shin.depthflow.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val WORKSPACE = "workspace"
    const val PRICING = "pricing"
    const val PROFILE = "profile"
    const val CONTACT_US = "contact_us"
    const val BILLING = "billing"
    const val FAQ = "faq"
    const val MY_PROJECTS = "my_projects"
    const val EDIT_PROFILE = "edit_profile"
    const val REGISTER = "register"
    const val VERIFY_OTP = "verify_otp"
    const val RESET_PASSWORD = "reset_password"
    const val EXPLORE_LIBRARY = "explore_library"
    const val HAPPY_HOUR = "happy_hour"
    const val NOTIFICATIONS = "notifications"

    // What's New detail page, opened from a banner in Notifications.
    const val WHATS_NEW_ARG = "releaseId"
    const val WHATS_NEW_DETAIL = "whats_new/{$WHATS_NEW_ARG}"
    fun whatsNewDetail(releaseId: Int) = "whats_new/$releaseId"
}
