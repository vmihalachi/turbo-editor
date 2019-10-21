/*
 * Copyright (C) 2014 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sharedcode.turboeditor.util;

import sharedcode.turboeditor.BuildConfig;

/**
 * Created by Artem on 30.12.13.
 */
public final class Build {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String SUPPORT_EMAIL = "maskyngames@gmail.com";

    public static final String GOOGLE_PLAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvpZca3gZSeRTHPMgxM+A1nTXuRL+9NTOWA1VJFs6ytppcO96i9EWQhmtXVUOKRQIgbXvkepxF7ut+JEjrbniQubZvmyBs9DxK7xUN4Zc3ZboQDQdfg2HJmZXzn8+joOfjXdS9WzsW7aaWKIQ8QXgOB1RUm9hdMdAlgKw+cEyp27WOoMK5m2H/i7C0MIO9tEQs3Hn9UTjayzzfy3MY+KDaX3T6oKievegbpyqyt8y4cpVusJC+uQFLa4bHKPtA3MaPUG6kU9tRV/DHrvFV6dOaPuTYCnYJELlGNfeqRUF0Nvb3Sv0U+BUoXgevjrlLdLz1bqgPDibLzaQmmofNXOnVQIDAQAB";

    public static final int MAX_FILE_SIZE = 20_000;

    public static final boolean FOR_AMAZON = false;

    public static class Links {

        public static final String GITHUB = "http://github.com/vmihalachi/TurboEditor";

        public static final String XDA = "http://forum.xda-developers.com/android/apps-games/app-turbo-editor-text-editor-t2832016";

        public static final String TRANSLATE = "http://crowdin.net/project/turbo-client";

        public static final String DONATE = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=26VWS2TSAMUJA";

        public static final String GOOGLE_PLUS_COMMUNITY = "http://plus.google.com/u/0/communities/111974095419108178946";

        public static final String AMAZON_STORE = "amzn://apps/android?p=com.maskyn.fileeditor";

        public static final String PLAY_STORE = "market://search?q=pub:Maskyn";

    }

}
