package com.example.grooveix

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.activity.PermissionActivity
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PermissionActivityTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_MEDIA_AUDIO
        } else {
            READ_EXTERNAL_STORAGE
        }
    )

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testPermissionGranted() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), PermissionActivity::class.java)
        ActivityScenario.launch<PermissionActivity>(intent)

        onView(withId(R.id.finishOnBoarding)).perform(click())

        Intents.intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun testPermissionDenied() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), PermissionActivity::class.java)
        ActivityScenario.launch<PermissionActivity>(intent)

        onView(withId(R.id.cancelBoarding)).perform(click())

        Intents.intended(allOf(
            hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            hasData("package:${ApplicationProvider.getApplicationContext<Context>().packageName}")
        ))
    }
}