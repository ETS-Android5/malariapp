/*
 * Copyright (c) 2016.
 *
 * This file is part of QA App.
 *
 *  Health Network QIS App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Health Network QIS App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare.test.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import com.google.android.gms.fitness.data.DataSet;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.eyeseetea.malariacare.LoginActivity;
import org.eyeseetea.malariacare.ProgressActivity;
import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.database.model.Option;
import org.eyeseetea.malariacare.database.model.Option$Table;
import org.eyeseetea.malariacare.database.model.OrgUnit;
import org.eyeseetea.malariacare.database.model.OrgUnit$Table;
import org.eyeseetea.malariacare.database.model.Program;
import org.eyeseetea.malariacare.database.model.Program$Table;
import org.eyeseetea.malariacare.database.model.Question;
import org.eyeseetea.malariacare.database.model.Survey;
import org.eyeseetea.malariacare.database.model.Survey$Table;
import org.eyeseetea.malariacare.utils.Constants;
import org.hamcrest.Matchers;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit$Table;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnitDataSet;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnitDataSet$Table;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnitGroup;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnitGroup$Table;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Created by arrizabalaga on 8/02/16.
 */
public class SDKTestUtils {

    public static final String HNQIS_DEV_STAGING = "https://hnqis-dev-staging.psi-mis.org";
    public static final String TEST_USERNAME_NO_PERMISSION = "testFAIL";
    public static final String TEST_PASSWORD_NO_PERMISSION = "testN0P3rmission";

    public static final String TEST_USERNAME_WITH_PERMISSION = "testOK";
    public static final String TEST_PASSWORD_WITH_PERMISSION = "testP3rmission";

    public static final String MARK_AS_COMPLETED = "Mark as completed";

    public static final String UNABLE_TO_LOGIN = "Unable to log in due to an invalid username or password.";


    public static void login(String server, String user, String password) {
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);

        //when: login
        onView(withId(org.hisp.dhis.android.sdk.R.id.server_url)).perform(replaceText(server));
        onView(withId(org.hisp.dhis.android.sdk.R.id.username)).perform(replaceText(user));
        onView(withId(org.hisp.dhis.android.sdk.R.id.password)).perform(replaceText(password));
        onView(withId(org.hisp.dhis.android.sdk.R.id.login_button)).perform(click());
    }

    public static void waitForPull(int secs) {
        //then: wait for progressactivity + dialog + ok (to move to dashboard)
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(secs * 1000);
        Espresso.registerIdlingResources(idlingResource);

        onView(withText(android.R.string.ok)).perform(click());

        Espresso.unregisterIdlingResources(idlingResource);
    }

    public static Survey waitForPush(int secs, Long idSurvey){
        //then: wait for pushservice
        try{
            Thread.sleep(secs*1000);
        }catch(Exception ex){
        }

        return Survey.findById(idSurvey);
    }

    public static void startSurvey(int idxOrgUnit, int idxProgram) {
        //when: click on assess tab + plus button
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        onView(withTagValue(Matchers.is((Object) getActivityInstance().getApplicationContext().getString(R.string.tab_tag_assess)))).perform(click());

        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        onView(withId(R.id.plusButton)).perform(click());

        //then: start survey 'test facility 1'+ 'family planning'+start


        onView(withId(R.id.org_unit)).perform(click());
        //Wait for service
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(5 * 1000);
        Espresso.registerIdlingResources(idlingResource);

        onData(is(instanceOf(OrgUnit.class))).atPosition(idxOrgUnit).perform(click());
        Espresso.unregisterIdlingResources(idlingResource);

        onView(withId(R.id.program)).perform(click());
        onData(is(instanceOf(Program.class))).atPosition(idxProgram).perform(click());

        onView(withId(R.id.create_form_button)).perform(click());

    }

    public static void fillSurvey(int numQuestions, String optionValue) {
        //when: answer NO to every question
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(5 * 1000);
        Espresso.registerIdlingResources(idlingResource);
        onView(withTagValue(Matchers.is((Object) getActivityInstance().getApplicationContext().getString(R.string.tab_tag_assess)))).perform(click());

        for (int i = 0; i < numQuestions; i++) {
            try {
                onData(is(instanceOf(Question.class)))
                        .inAdapterView(withId(R.id.listView))
                        .atPosition(i)
                        .onChildView(withId(R.id.answer))
                        .onChildView(withText(optionValue))//.onChildView(withTagValue(allOf(Matchers.hasProperty("name", containsString(optionValue)))))
                        .perform(click());


            }catch (Exception e){}
        }

        Espresso.unregisterIdlingResources(idlingResource);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        //then: back + confirm
        Espresso.pressBack();
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        onView(withText(android.R.string.ok)).perform(click());
    }

    public static Long markInProgressAsCompleted() {
        Long idSurvey = getSurveyId();

        //when: Mark as completed
        onView(withId(R.id.score)).perform(click());
        onView(withText(MARK_AS_COMPLETED)).perform(click());
        return idSurvey;
    }

    private static Long getSurveyId(){
        return getSurveyInProgress().getId_survey();
    }

    public static Option getOption(int Survey, String value){
        return new Select()
                .from(Option.class)
                .where(Condition.column(Option$Table.NAME)
                        .eq(value))
                .querySingle();
    }
    private static Survey getSurveyInProgress(){
        return new Select()
                .from(Survey.class)
                .where(Condition.column(Survey$Table.STATUS)
                        .eq(Constants.SURVEY_IN_PROGRESS))
                .querySingle();
    }
    public static  OrgUnit getOrgUnit(String id){
        return new Select()
                .from(OrgUnit.class)
                .where(Condition.column(OrgUnit$Table.UID)
                        .eq(id))
                .querySingle();
    }
    public static OrganisationUnit getOrganisationUnit(String id){
        return new Select()
                .from(OrganisationUnit.class)
                .where(Condition.column(OrganisationUnit$Table.ID)
                        .eq(id))
                .querySingle();
    }

    public static List<OrganisationUnitGroup> getOrganisationUnitGroups(String id){
        return new Select()
                .from(OrganisationUnitGroup.class)
                .where(Condition.column(OrganisationUnitGroup$Table.ORGANISATIONUNITID)
                        .eq(id))
                .queryList();
    }

    public static List<OrganisationUnitDataSet> getOrganisationUnitDataSets(String id){
        return new Select()
                .from(OrganisationUnitDataSet.class)
                .where(Condition.column(OrganisationUnitDataSet$Table.ORGANISATIONUNITID)
                        .eq(id))
                .queryList();
    }

    public static org.hisp.dhis.android.sdk.persistence.models.Program getSDKProgram(String id){
        return new Select()
                .from(org.hisp.dhis.android.sdk.persistence.models.Program.class)
                .where(Condition.column(org.hisp.dhis.android.sdk.persistence.models.Program$Table.ID)
                        .eq(id))
                .querySingle();
    }

    public static List<org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit> getAllSDKOrganisationUnits() {
        return new Select().all().from(org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit.class).queryList();
    }

    public static List<org.hisp.dhis.android.sdk.persistence.models.Program> getAllSDKPrograms() {
        return new Select().all().from(org.hisp.dhis.android.sdk.persistence.models.Program.class).queryList();
    }

    public static Activity getActivityInstance() {
        final Activity[] activity = new Activity[1];
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                if (resumedActivities.iterator().hasNext()) {
                    activity[0] = (Activity) resumedActivities.iterator().next();
                }
            }
        });

        return activity[0];
    }


    public static void goToLogin(){
        try{
            if(LoginActivity.class.equals(SDKTestUtils.getActivityInstance().getClass())){
                return;
            }
            else{
                if(ProgressActivity.class.equals(SDKTestUtils.getActivityInstance().getClass())){
                    try{
                        onView(withText(android.R.string.cancel)).perform(click());
                    }catch (Exception e){}

                }
                else{
                    Espresso.pressBack();
                    try {
                        onView(withText(android.R.string.ok)).perform(click());
                    } catch (Exception e) {
                    }
                }
            }
        } catch(Exception e){}
        goToLogin();
    }
}
