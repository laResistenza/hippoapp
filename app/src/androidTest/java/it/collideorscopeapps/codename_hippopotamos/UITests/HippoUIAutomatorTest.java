package it.collideorscopeapps.codename_hippopotamos.UITests;

import org.junit.Before;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.Suppress;
import static androidx.test.runner.lifecycle.Stage.RESUMED;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import it.collideorscopeapps.codename_hippopotamos.MainActivity;
import it.collideorscopeapps.codename_hippopotamos.R;


//@RunWith(AndroidJUnit4.class)//@SdkSuppress(minSdkVersion = 18)
public class HippoUIAutomatorTest {

    private static final String BASIC_SAMPLE_PACKAGE
            = "it.collideorscopeapps.codename_hippopotamos";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String STRING_TO_BE_TYPED = "UiAutomator";
    private UiDevice device;

    @Before
    public void startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
                LAUNCH_TIMEOUT);

        //interact with app
        UiObject cancelButton = device.findObject(new UiSelector()
                .text("Cancel")
                .className("android.widget.Button"));
        UiObject okButton = device.findObject(new UiSelector()
                .text("OK")
                .className("android.widget.Button"));

        // Simulate a user-click on the OK button, if found.
        try {
            if(okButton.exists() && okButton.isEnabled()) {
                okButton.click();
            }
        } catch (UiObjectNotFoundException e) {

        }

        // As a best practice, when specifying a selector, you
        // should use a Resource ID (if one is assigned to a UI
        // element) instead of a text element or content-descriptor.
        UiObject appItem = device.findObject(new UiSelector()
                .className("android.widget.ListView")
                .instance(0)
                .childSelector(new UiSelector()
                        .text("Apps")));

        //appItem. swipeLeft();
    }

    public void alternativeSetUp() {
    //...

        final String HIPPO_PACKAGE = "it.collideorscopeapps.codename_hippopotamos";
        final int TIMEOUT = 500;

        // Launch a simple calculator app
        Context context = getInstrumentation().getContext();
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(HIPPO_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Clear out any previous instances
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg(HIPPO_PACKAGE).depth(0)), TIMEOUT);

        //https://developer.android.com/training/testing/ui-testing/uiautomator-testing?authuser=1

        //Testing fragments
        //https://developer.android.com/training/basics/fragments/testing?authuser=1
    }

}
