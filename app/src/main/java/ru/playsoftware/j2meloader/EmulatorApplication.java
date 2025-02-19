/*
 * Copyright 2017-2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ru.playsoftware.j2meloader.util.Constants;
import ru.playsoftware.j2meloader.util.FileUtils;

public class EmulatorApplication extends Application {
	private static final byte[] SIGNATURE_SHA = {
			125, 47, 64, 33, 91, -86, -121, 89, 11, 24, -118, -93, 35, 53, -34, -114, -119, -60, -48, 55
	};

	private static EmulatorApplication instance;

	private final SharedPreferences.OnSharedPreferenceChangeListener themeListener = (sharedPreferences, key) -> {
		if (key.equals(Constants.PREF_THEME)) {
			setNightMode(sharedPreferences.getString(Constants.PREF_THEME, null));
		}
	};
	public static EmulatorApplication getInstance() {
		return instance;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		instance = this;
		if (BuildConfig.DEBUG) {
			MultiDex.install(this);
		}

		ACRA.init(this, new CoreConfigurationBuilder()
				.withBuildConfigClass(BuildConfig.class)
				.withParallel(false)
				.withSendReportsInDevMode(false)
				.withPluginConfigurations(new DialogConfigurationBuilder()
						.withTitle(getString(R.string.crash_dialog_title))
						.withText(getString(R.string.crash_dialog_message))
						.withPositiveButtonText(getString(R.string.report_crash))
						.withResTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog)
						.withEnabled(true)
						.build()
				));
		ACRA.getErrorReporter().setEnabled(isSignatureValid());
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(themeListener);
		setNightMode(sp.getString(Constants.PREF_THEME, null));
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
	}

	@NonNull
	public static String getProcessName() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			return Application.getProcessName();
		} else {
			return FileUtils.getText("/proc/self/cmdline").trim();
		}
	}

	@SuppressLint("PackageManagerGetSignatures")
	private boolean isSignatureValid() {
		try {
			Signature[] signatures;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				PackageInfo info = getPackageManager()
						.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
				signatures = info.signingInfo.getApkContentsSigners();
			} else {
				PackageInfo info = getPackageManager()
						.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
				signatures = info.signatures;
			}
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			for (Signature signature : signatures) {
				md.update(signature.toByteArray());
				if (MessageDigest.isEqual(SIGNATURE_SHA, md.digest())) {
					return true;
				}
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;
	}

	void setNightMode(String theme) {
		if (theme == null) {
			theme = getString(R.string.pref_theme_default);
		}
		switch (theme) {
			case "light":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case "dark":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
			case "auto-battery":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
				break;
			case "auto-time":
				//noinspection deprecation
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME);
				break;
			default:
			case "system":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
				break;
		}
	}
}
