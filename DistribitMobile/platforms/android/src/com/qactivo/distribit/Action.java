/*
 * Copyright (C) 2015 Baldani Sergio - Tardivo Cristian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.qactivo.distribit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Basic annotated interface for action methods.
 * @author Cristian Tardivo
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {

    /**
     * Available actions
     */
    public enum List {
        NONE, CHECK_DEVICE_ONLINE, ATTACH_ACC_MGR, CREATE_ACCOUNT_POLLING, ATTACH_PROJECT, CHECK_PROJECT_ATTACHED, SET_MANUALLY_SELECTED_PROJECT,
        GET_NEXT_SELECTED_PROJECT, GET_SELECTED_PROJECTS, GET_NUMBER_SELECTED_PROJECTS, SET_CREDENTIALS, LOGIN, REGISTER,
        LOOKUP_AND_ATTACH, GET_USER_DEFAULT_VALUES, SET_SELECTED_PROJECTS, PROJECT_CONFIG_RETRIEVAL_FINISHED, GET_PROJECTS,
        SET_RUN_MODE, SET_NETWORK_MODE, GET_COMPUTING_STATUS, GET_CLIENT_PREFS, SET_CLIENT_PREFS, GET_HOST_INFO, GET_PROJECT_INFO,
        PROJECT_OP, GET_PROJECT_CONFIG_POLLING, GET_TRANSFERS, GET_SERVER_NOTICES, GET_TASKS, RESULT_OP, LOOKUP_CREDENTIALS, GET_AUTOSTART,
        GET_CURRENT_STATUS_DESCRIPTION, GET_CURRENT_STATUS_TITLE, GET_BATTERY_CHARGE_STATUS, GET_COMPUTING_SUSPEND_REASON, GET_NETWORK_SUSPEND_REASON,
        GET_SUSPEND_WHEN_SCREEN_ON, GET_POWER_SOURCE_AC, GET_POWER_SOURCE_USB, GET_POWER_SOURCE_WIRELESS, SET_AUTOSTART, SET_SUSPEND_WHEN_SCREEN_ON,
        SET_POWER_SOURCE_AC, SET_POWER_SOURCE_USB, SET_POWER_SOURCE_WIRELESS, RUN_BENCHMARKS, FORCE_REFRESH, BOINC_MUTEX_ACQUIRED,
        GET_PROJECT_STATUS, GET_ATTACHABLE_PROJECTS, DETERMINE_RESULT_STATUS_TEXT, SYNCHRONIZE_ACCT_MGR, GET_ACCT_MGR_INFO, GET_CLIENT_ACCT_MGR_INFO, HAS_UNRESOLVED_CONFLICTS
    }

    /**
     * Annotation value
     * @return
     */
    List value() default List.NONE;
}


