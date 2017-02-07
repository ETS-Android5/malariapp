/*
 * Copyright (c) 2015.
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

package org.eyeseetea.malariacare.data.database.model;

import static org.eyeseetea.malariacare.data.database.AppDatabase.matchAlias;
import static org.eyeseetea.malariacare.data.database.AppDatabase.matchName;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionAlias;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionName;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionOptionAlias;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionOptionName;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionRelationAlias;
import static org.eyeseetea.malariacare.data.database.AppDatabase.questionRelationName;
import static org.eyeseetea.malariacare.data.database.AppDatabase.valueAlias;
import static org.eyeseetea.malariacare.data.database.AppDatabase.valueName;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Update;
import com.raizlabs.android.dbflow.sql.language.Where;
import com.raizlabs.android.dbflow.sql.language.property.Property;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.eyeseetea.malariacare.data.database.AppDatabase;
import org.eyeseetea.malariacare.data.database.iomodules.dhis.exporter.IConvertToSDKVisitor;
import org.eyeseetea.malariacare.data.database.iomodules.dhis.exporter.VisitableToSDK;
import org.eyeseetea.malariacare.data.database.utils.SurveyAnsweredRatio;
import org.eyeseetea.malariacare.data.database.utils.SurveyAnsweredRatioCache;
import org.eyeseetea.malariacare.data.database.utils.planning.SurveyPlanner;
import org.eyeseetea.malariacare.layout.score.ScoreRegister;
import org.eyeseetea.malariacare.utils.Constants;
import org.hisp.dhis.client.sdk.android.api.persistence.flow.EventFlow;
import org.hisp.dhis.client.sdk.android.api.persistence.flow.EventFlow_Table;

import java.util.Date;
import java.util.List;

@Table(database = AppDatabase.class)
public class Survey extends BaseModel implements VisitableToSDK {

    @Column
    @PrimaryKey(autoincrement = true)
    long id_survey;

    @Column
    Long id_program;
    /**
     * Reference to the program associated to this survey (loaded lazily)
     */
    Program program;

    @Column
    Long id_org_unit;
    /**
     * Reference to the org unit associated to this survey (loaded lazily)
     */
    OrgUnit orgUnit;

    @Column
    Long id_user;
    /**
     * Reference to the user that has created this survey (loaded lazily)
     */
    User user;

    @Column
    Date creation_date;

    @Column
    Date completion_date;

    @Column
    Date upload_date;

    @Column
    Date scheduled_date;

    @Column
    Integer status;

    @Column
    String eventuid;

    /**
     * List of values for this survey
     */
    List<Value> values;

    /**
     * List of historic previous schedules
     */
    List<SurveySchedule> surveySchedules;

    /**
     * Calculated answered ratio for this survey according to its values
     */
    SurveyAnsweredRatio answeredQuestionRatio;

    /**
     * Calculated main Score for this survey, is not persisted, just calculated on runtime
     */
    Float mainScore;

    /**
     * hasMainScore is used to know if the survey have a compositeScore with only 1 query time.
     */
    private Boolean hasMainScore=null;

    /**
     * Expected productivity for this survey according to its orgunit + program.
     * Just a cached value from orgunitprogramproductivity
     */
    Integer productivity;

    public Survey() {
        //Set dates
        this.creation_date = new Date();
        this.completion_date = null;
        this.upload_date = null;
        this.scheduled_date = null;
    }

    public Survey(OrgUnit orgUnit, Program program, User user) {
        this();

        // Possibilities [ In progress | Completed | Sent ]
        this.status = Constants.SURVEY_IN_PROGRESS;

        //Set context of the survey
        this.setOrgUnit(orgUnit);
        this.setProgram(program);
        this.setUser(user);
    }

    public Long getId_survey() {
        return id_survey;
    }

    public void setId_survey(Long id_survey) {
        this.id_survey = id_survey;
    }

    public String getEventUid() {
        return eventuid;
    }

    public void setEventUid(EventFlow event) {
        this.eventuid = event.getUId();
    }

    public void setEventUid(String eventuid) {
        this.eventuid = eventuid;
    }

    public OrgUnit getOrgUnit() {
        if(orgUnit==null){
            if (id_org_unit==null) return null;
            orgUnit = new Select().from(OrgUnit.class)
                    .where(OrgUnit_Table.id_org_unit
                            .is(id_org_unit)).querySingle();
        }
        return orgUnit;
    }

    public void setOrgUnit(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
        this.id_org_unit = (orgUnit!=null)?orgUnit.getId_org_unit():null;
    }

    public void setOrgUnit(Long id_org_unit){
        this.id_org_unit = id_org_unit;
        this.orgUnit = null;
    }

    public Program getProgram() {
        if(program == null){
            if (id_program == null) return null;
            program = new Select()
                    .from(Program.class)
                    .where(Program_Table.id_program
                            .is(id_program)).querySingle();
        }
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
        this.id_program = (program!=null)?program.getId_program():null;
    }

    public void setProgram(Long id_program){
        this.id_program = id_program;
        this.id_program= null;
    }

    public User getUser() {
        if(user==null){
            if(id_user==null) return null;
            user= new Select()
                    .from(User.class)
                    .where(User_Table.id_user
                            .is(id_user)).querySingle();
        }
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.id_user = (user!=null)?user.getId_user():null;
    }

    public void setUser(Long id_user){
        this.id_user = id_user;
        this.user = null;
    }

    public Date getCreationDate() {
        return creation_date;
    }

    public void setCreationDate(Date creation_date) {
        this.creation_date = creation_date;
    }

    public Date getCompletionDate(){
        return completion_date;
    }

    public void setCompletionDate(Date completion_date){
        this.completion_date = completion_date;
    }

    public Date getUploadDate() {
        return upload_date;
    }

    public void setUploadDate(Date upload_date) {
        this.upload_date = upload_date;
    }

    public Date getScheduledDate() {
        return scheduled_date;
    }

    public void setScheduledDate(Date scheduled_date) {
        this.scheduled_date = scheduled_date;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * Checks if the survey has been sent or not
     * @return true|false
     */
    public boolean isSent(){
        return Constants.SURVEY_SENT==this.status;
    }

    /**
     * Checks if the survey is PLANNED
     * @return true|false
     */
    public boolean isPlanned(){
        return Constants.SURVEY_PLANNED==this.status;
    }

    /**
     * Checks if the survey is IN_PROGRESS
     * @return true|false
     */
    public boolean isInProgress(){
        return Constants.SURVEY_IN_PROGRESS==this.status;
    }

    /**
     * Checks if the survey has been completed or not
     * @return true|false
     */
    public boolean isCompleted(){
        return Constants.SURVEY_COMPLETED==this.status;
    }

    public boolean isConflict() {
        return Constants.SURVEY_CONFLICT==this.status;
    }

    public boolean isReadOnly() {
        return (isCompleted() || isSent());
    }

    public Float getMainScore() {
        //The main score is only return from a query 1 time
        if(this.mainScore==null){
            Score score=getScore();
            this.mainScore=(score==null)?0f:score.getScore();
        }
        return mainScore;
    }

    public Boolean hasMainScore() {
        if(hasMainScore==null || !hasMainScore) {
            Score score = getScore();
            Float value = (score == null) ? null : score.getScore();
            if (value == null) {
                hasMainScore = false;
            }
            else {
                hasMainScore = true;
            }
        }
        return hasMainScore;
    }

    public void setMainScore(Float mainScore) {
        this.mainScore = mainScore;
    }

    public void saveMainScore(){
        Float valScore=0f;
        if(mainScore!=null){
            valScore=mainScore;
        }
        //Update or New row
        Score score=getScore();
        if(score==null){
            score=new Score(this,"",valScore);
        }else{
            score.setScore(valScore);
        }
        score.save();
    }

    private Score getScore(){
        return new Select()
                .from(Score.class)
                .where( Score_Table.id_survey.eq(this.getId_survey())).querySingle();
    }

    /**
     * Returns the productivity for this survey according to its orgunit + program
     * @return
     */
    public Integer getProductivity(){
        if(productivity==null){
            productivity = OrgUnitProgramRelation.getProductivity(this);
        }
        return productivity;
    }

    /**
     * Returns if this survey has low productivity or not.
     * [0..4]: Low
     * [5..): Not Low
     * @return
     */
    public boolean isLowProductivity(){
        return getProductivity()<5;
    }

    @Override
    public void delete(){
        Score score=getScore();
        if(score!=null){
            score.delete();
        }
        super.delete();
    }

    public String getType(){
        String type = "";
        if (isTypeA()) type = "A";
        else if (isTypeB()) type = "B";
        else if (isTypeC()) type = "C";
        return type;
    }
    /**
     * Returns this survey is type A (green)
     * @return
     */
    public boolean isTypeA(){
        return this.getMainScore()>= Constants.MAX_AMBER;
    }

    /**
     * Returns this survey is type B (amber)
     * @return
     */
    public boolean isTypeB(){
        return this.getMainScore()>= Constants.MAX_RED && !isTypeA();
    }

    /**
     * Returns this survey is type C (red)
     * @return
     */
    public boolean isTypeC(){
        return !isTypeA() && !isTypeB();
    }

    /**
     * Returns the list of answered values from this survey
     * @return
     */
    public List<Value> getValues(){
        if(values==null){
            values = new Select()
                    .from(Value.class)
                    .where(Value_Table.id_survey
                            .eq(this.getId_survey())).queryList();
        }
        return values;
    }

    /**
     * Returns the list of previous schedules for this survey
     * @return
     */
    public List<SurveySchedule> getSurveySchedules(){
        if(surveySchedules==null){
            surveySchedules = new Select()
                    .from(SurveySchedule.class)
                    .where(SurveySchedule_Table.id_survey
                            .eq(this.getId_survey())).queryList();
        }
        return surveySchedules;
    }

    /**
     * Update the SurveySchedule to be set in a new survey, and could remove the old survey without lost that
     * @param newSurvey to set the SurveSchedule
     */
    public void setSurveyScheduleToSurvey(Survey newSurvey){
        Where update = new Update<>(SurveySchedule.class).set(SurveySchedule_Table.id_survey.eq(newSurvey.getId_survey()))
                .where(SurveySchedule_Table.id_survey.is(id_survey));
        //old update.queryClose();
        update.query();
    }

    /**
     * Returns the list of answered values from this survey that belong to a parent question
     * @return
     */
    public List<Value> getValuesFromParentQuestions(){
        List<Value> values = new Select().from(Value.class).as("v")
                .join(Question.class, Join.JoinType.LEFT_OUTER).as("q")
                .on(Value_Table.id_question.withTable(NameAlias.builder("v").build())
                        .eq(Question_Table.id_question.withTable(NameAlias.builder("q").build())))
                .where(Value_Table.id_survey.withTable(NameAlias.builder("v").build())
                        .eq(this.getId_survey()))
                .and(Question_Table.id_parent.withTable(NameAlias.builder("q").build()).isNull())
                .and(Value_Table.value.withTable(NameAlias.builder("v").build()).isNotNull())
                .and(Value_Table.value.withTable(NameAlias.builder("v").build()).isNot("")).queryList();
        //List<Value> values = Value.findWithQuery(Value.class, LIST_VALUES_PARENT_QUESTION, this.getId().toString());
        return values;
    }

    /**
     * Ratio of completion is cached into answeredQuestionRatio in order to speed up loading
     * @return
     */
    public SurveyAnsweredRatio getAnsweredQuestionRatio(){
        if (answeredQuestionRatio == null) {
            answeredQuestionRatio = SurveyAnsweredRatioCache.get(this.getId_survey());
            if(answeredQuestionRatio == null) {
                answeredQuestionRatio = reloadSurveyAnsweredRatio();
            }
        }
        return answeredQuestionRatio;
    }

    /**
     * Calculates the current ratio of completion for this survey
     * @return SurveyAnsweredRatio that hold the total & answered questions.
     */
    public SurveyAnsweredRatio reloadSurveyAnsweredRatio(){
        //TODO Review
        Program surveyProgram = this.getProgram();
        int numRequired = Question.countRequiredByProgram(surveyProgram);
        int numCompulsory=Question.countCompulsoryByProgram(surveyProgram);
        int numOptional = (int)countNumOptionalQuestionsToAnswer();
        int numActiveChildrenCompulsory = Question.countChildrenCompulsoryBySurvey(this.id_survey);
        int numAnswered = Value.countBySurvey(this);
        int numCompulsoryAnswered = Value.countCompulsoryBySurvey(this);
        SurveyAnsweredRatio surveyAnsweredRatio=new SurveyAnsweredRatio(numRequired+numOptional, numAnswered,numCompulsory+numActiveChildrenCompulsory,numCompulsoryAnswered);
        SurveyAnsweredRatioCache.put(this.id_survey, surveyAnsweredRatio);
        return surveyAnsweredRatio;
    }

    /**
     * Return the number of child questions that should be answered according to the values of the parent questions.
     * @return
     */
    private long countNumOptionalQuestionsToAnswer(){
     long numOptionalQuestions = SQLite.selectCountOf().from(Question.class).as(questionName)
                .join(QuestionRelation.class, Join.JoinType.LEFT_OUTER).as(questionRelationName)
                .on(Question_Table.id_question.withTable(questionAlias)
                        .eq(QuestionRelation_Table.id_question.withTable(questionRelationAlias)))
                .join(Match.class, Join.JoinType.LEFT_OUTER).as(matchName)
                .on(QuestionRelation_Table.id_question_relation.withTable(questionRelationAlias)
                                .eq(Match_Table.id_question_relation.withTable(matchAlias)))
                .join(QuestionOption.class, Join.JoinType.LEFT_OUTER).as(questionOptionName)
                .on(Match_Table.id_match.withTable(matchAlias)
                                .eq(QuestionOption_Table.id_match.withTable(questionOptionAlias)))
                .join(Value.class, Join.JoinType.LEFT_OUTER).as(valueName)
                .on(Value_Table.id_question.withTable(valueAlias)
                                .eq(QuestionOption_Table.id_question.withTable(questionOptionAlias)),
                        Value_Table.id_option.withTable(valueAlias)
                                .eq(QuestionOption_Table.id_option.withTable(questionOptionAlias)))
                //Parent Child relationship
                .where(QuestionRelation_Table.operation.withTable(questionRelationAlias).eq(QuestionRelation.PARENT_CHILD))
                //For the given survey
                .and( Value_Table.id_survey.withTable(valueAlias).eq(this.getId_survey()))
                //The child question requires an answer
                .and(Question_Table.output.withTable(questionAlias).isNot(Constants.NO_ANSWER))
                .count();
        //Parent with the right value -> not hidden
        return numOptionalQuestions;
    }

    /**
     * Updates ratios, status and completion date depending on the question and answer (text)
     */
    public void updateSurveyStatus(){

        //Exit if the survey was sent or completed
        if(isReadOnly()){
            return;
        }


        SurveyAnsweredRatio answeredRatio = this.reloadSurveyAnsweredRatio();

        SurveyAnsweredRatio surveyAnsweredRatio = this.getAnsweredQuestionRatio();
        if (surveyAnsweredRatio.getTotalCompulsory()==0) {
            //Update status
            if(!answeredRatio.isCompleted())
            this.setStatus(Constants.SURVEY_IN_PROGRESS);

        }
        else if(surveyAnsweredRatio.getCompulsoryAnswered()==0){
            this.setStatus(Constants.SURVEY_IN_PROGRESS);
        }


        //Saves new status & completion_date
        this.save();
    }

    private void saveScore(String module) {        //Prepare scores info
        List<CompositeScore> compositeScoreList= ScoreRegister.loadCompositeScores(this, module);

        //Calculate main score to push later
        this.setMainScore(ScoreRegister.calculateMainScore(compositeScoreList, id_survey, module));
        this.saveMainScore();
    }

    /**
     * Returns a survey in progress for the given orgUnit and program
     * @param orgUnit
     * @param program
     * @return
     */
    public static Survey getInProgressSurveys(OrgUnit orgUnit, Program program) {
        if(orgUnit==null || program==null){
            return null;
        }

        return new Select().from(Survey.class)
                .where(Survey_Table.id_org_unit.eq(orgUnit.getId_org_unit()))
                .and(Survey_Table.id_program.eq(program.getId_program()))
                .and(Survey_Table.status.is(Constants.SURVEY_IN_PROGRESS))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).querySingle();
    }

    /**
     * Returns all the surveys with status yet not put to "Sent"
     * @return
     */
    public static List<Survey> getAllUnsentUnplannedSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.isNot(Constants.SURVEY_SENT))
                .and(Survey_Table.status.isNot(Constants.SURVEY_PLANNED))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    /**
     * Returns the last surveys (by date) with status yet not put to "Sent"
     * @param limit
     * @return
     */
    public static List<Survey> getUnsentSurveys(int limit) {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.isNot(Constants.SURVEY_SENT))
                .limit(limit)
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    /**
     * Returns all the surveys with status put to "Sent"
     * @return
     */
    public static List<Survey> getAllSentSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_SENT))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    /**
     * Returns the last surveys (by date) with status put to "Sent"
     * @param limit
     * @return
     */
    public static List<Survey> getSentSurveys(int limit) {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_SENT))
                .limit(limit)
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    /**
     * Returns all the surveys with status put to "Completed"
     * @return
     */
    public static List<Survey> getAllCompletedSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_COMPLETED))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    /**
     * Returns the last surveys (by date) with status put to "Completed"
     * @param limit
     * @return
     */
    public static List<Survey> getCompletedSurveys(int limit) {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_COMPLETED))
                .limit(limit)
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }


    /**
     * Returns the last surveys (by date) with status put to "In progress"
     * @param limit
     * @return
     */
    public static List<Survey> getUncompletedSurveys(int limit) {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_IN_PROGRESS))
                .limit(limit)
                .orderBy(OrderBy.fromProperty(Survey_Table.upload_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    @Override
    public void accept(IConvertToSDKVisitor IConvertToSDKVisitor) throws Exception{
        IConvertToSDKVisitor.visit(this);
    }

    /**
     * Returns the last surveys (by date) without status Completed or sent
     * @return
     */
    public static List<Survey> getAllInProgressSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.is(Constants.SURVEY_IN_PROGRESS))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }
    /**
     * Returns the last surveys (by date) with status Completed or sent
     * @return
     */
    public static List<Survey> getAllCompletedUnsentSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.is(Constants.SURVEY_COMPLETED))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }
    /**
     * Returns all the surveys with status put to "Sent" or completed or Conflict
     * @return
     */
    public static List<Survey> getAllSentCompletedOrConflictSurveys() {
        return new Select().from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_SENT))
                .or(Survey_Table.status.eq(Constants.SURVEY_COMPLETED))
                .or(Survey_Table.status.eq(Constants.SURVEY_CONFLICT))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit)).queryList();
    }

    public static long count(){
        return SQLite.selectCountOf()
                .from(Survey.class)
                .count();
    }

    public void saveConflict(String uid){
        for(Value value:getValues()){
            if(value.getQuestion().getUid().equals(uid)){
                value.setConflict(true);
                value.save();
            }
        }
    }

    public boolean hasConflict(){
        for(Value value:getValues()){
            if(value.getConflict()){
                return true;
            }
        }
        return false;
    }

    public void prepareSurveyUploadedDate() {
        if (!isSent()) {
            setUploadDate(new Date());
            save();
        }
    }

    public void setSentSurveyState(){
        //Change status and save mainScore
        setStatus(Constants.SURVEY_SENT);
        save();
        saveMainScore();
    }

    public void setCompleteSurveyState(String module){
        setStatus(Constants.SURVEY_COMPLETED);
        //CompletionDate
        this.setCompletionDate(new Date());
        saveScore(module);
        save();
        saveMainScore();
        //Plan a new survey for the future
        SurveyPlanner.getInstance().buildNext(this);
    }

    /**
     * Moves the scheduled date for this survey to a new given date due to a given reason (comment)
     * @param newScheduledDate
     * @param comment
     */
    public void reschedule(Date newScheduledDate, String comment) {
        //Take currentDate
        Date currentScheduledDate=this.getScheduledDate();

        //Add a history
        SurveySchedule previousSchedule=new SurveySchedule(this,currentScheduledDate,comment);
        previousSchedule.save();

        //Clean inner lazy schedulelist
        surveySchedules=null;

        //Move scheduledate and save
        this.scheduled_date =newScheduledDate;
        this.save();
    }

    /**
     * Finds a survey by its ID
     * @param id_survey
     * @return
     */
    public static Survey findById(Long id_survey){
        return new Select()
                .from(Survey.class)
                .where(Survey_Table.id_survey
                        .eq(id_survey))
                .querySingle();
    }

    public static boolean exists(Long id_survey){
        return findById(id_survey)!=null;
    }

    /**
     * Returns all surveys which status is 'planned' or 'in progress'
     * @return
     */
    public static List<Survey> findPlannedOrInProgress(){
        return new Select()
                .from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_PLANNED))
                .or(Survey_Table.status.eq(Constants.SURVEY_IN_PROGRESS))
                .orderBy(Survey_Table.scheduled_date, true)
                .queryList();
    }

    /**
     * Returns survey which state is 'in progress' or 'sent'
     * @return
     */
    public static List<Survey> findInProgressOrSent() {
        return new Select()
                .from(Survey.class)
                .where(Survey_Table.status.eq(Constants.SURVEY_IN_PROGRESS))
                .or(Survey_Table.status.eq(Constants.SURVEY_SENT))
                .orderBy(OrderBy.fromProperty(Survey_Table.completion_date))
                .orderBy(OrderBy.fromProperty(Survey_Table.id_org_unit))
                .queryList();
    }

    /**
     * Finds a survey with a given orgunit and program
     * @param orgUnit
     * @param program
     * @return
     */
    public static Survey findByOrgUnitAndProgram(OrgUnit orgUnit, Program program) {
        return new Select()
                .from(Survey.class)
                .where(Survey_Table.id_org_unit.eq(orgUnit.getId_org_unit()))
                .and(Survey_Table.id_program.eq(program.getId_program()))
                .and(Survey_Table.status.eq(Constants.SURVEY_PLANNED))
                .querySingle();
    }

    /**
     * Find the last survey that has been sent for each orgunit+program combination
     * @return
     */
    public static List<Survey> listLastByOrgUnitProgram() {
        return SQLite.select()
                .from(Survey.class)
                .where()
                .groupBy( Survey_Table.id_org_unit, Survey_Table.id_program)
                .having(Survey_Table.completion_date.eq(Method.max(Survey_Table.completion_date)))
                .queryList();
    }


    public static Survey findSurveyWith(OrgUnit orgUnit, Program program,EventFlow lastEventInServer) {

        if(lastEventInServer==null){
            return null;
        }
        return new Select().from(Survey.class)
                .where(Survey_Table.status.isNot(Constants.SURVEY_PLANNED))
                .and(Survey_Table.status.isNot(Constants.SURVEY_IN_PROGRESS))
                .and(Survey_Table.status.isNot(Constants.SURVEY_HIDE))
                .and( Survey_Table.id_program.eq(program.getId_program()))
                .and(Survey_Table.id_org_unit.eq(orgUnit.getId_org_unit()))
                .and(Survey_Table.eventuid.eq(lastEventInServer.getUId()))
                .orderBy(Survey_Table.completion_date,false)
                .querySingle();
    }

    public static Survey getLastSurvey(Long id_org_unit, Long id_program) {
        return  SQLite.select()
                .from(Survey.class)
                .where(Survey_Table.id_program.eq(id_program))
                .and(Survey_Table.id_org_unit.eq(id_org_unit))
                .groupBy( Survey_Table.id_program , Survey_Table.id_org_unit)
                .having(Survey_Table.completion_date.eq(Method.max(Survey_Table.completion_date)))
                .querySingle();
    }

    /**
     * A survey in progress with a uid is a modification (patch) of a previously pushed survey
     * @return
     */
    public boolean isAModification(){
        return this.eventuid!=null && this.eventuid.length()>0;
    }

    /**
     * Get event from a survey if exists.
     * @return
     */
    public EventFlow getEvent(){
        return new Select().from(EventFlow.class)
                .where(EventFlow_Table.uId.eq(eventuid)).querySingle();
    }

    /**
     * Get event from a survey local id if exist
     * @return
     */
    public EventFlow getEventFromLocalId(){
        EventFlow event= new Select().from(EventFlow.class)
                .where(EventFlow_Table.id.eq( id_survey)).querySingle();
        return event;
    }

    public static Survey getSurveyInProgress(){
        return new Select()
                .from(Survey.class)
                .where(Survey_Table.status
                        .eq(Constants.SURVEY_IN_PROGRESS))
                .querySingle();
    }

    public String getFullName(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getOrgUnit().getName());
        stringBuilder.append(", ");
        stringBuilder.append(this.getProgram().getName());
        return stringBuilder.toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Survey survey = (Survey) o;

        if (id_survey != survey.id_survey) return false;
        if (id_program != null ? !id_program.equals(survey.id_program) : survey.id_program != null)
            return false;
        if (id_org_unit != null ? !id_org_unit.equals(survey.id_org_unit) : survey.id_org_unit != null)
            return false;
        if (id_user != null ? !id_user.equals(survey.id_user) : survey.id_user != null)
            return false;
        if (creation_date != null ? !creation_date.equals(survey.creation_date) : survey.creation_date != null)
            return false;
        if (completion_date != null ? !completion_date.equals(survey.completion_date) : survey.completion_date != null)
            return false;
        if (upload_date != null ? !upload_date.equals(survey.upload_date) : survey.upload_date != null)
            return false;
        if (eventuid != null ? !eventuid.equals(survey.eventuid) : survey.eventuid != null)
            return false;
        if (scheduled_date != null ? !scheduled_date.equals(survey.scheduled_date) : survey.scheduled_date != null)
            return false;
        return !(status != null ? !status.equals(survey.status) : survey.status != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id_survey ^ (id_survey >>> 32));
        result = 31 * result + (id_program != null ? id_program.hashCode() : 0);
        result = 31 * result + (id_org_unit != null ? id_org_unit.hashCode() : 0);
        result = 31 * result + (id_user != null ? id_user.hashCode() : 0);
        result = 31 * result + (creation_date != null ? creation_date.hashCode() : 0);
        result = 31 * result + (completion_date != null ? completion_date.hashCode() : 0);
                result = 31 * result + (upload_date != null ? upload_date.hashCode() : 0);
        result = 31 * result + (eventuid != null ? eventuid.hashCode() : 0);
        result = 31 * result + (scheduled_date != null ? scheduled_date.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Survey{" +
                "id_survey=" + id_survey +
                ", id_program=" + id_program +
                ", id_org_unit=" + id_org_unit +
                ", id_user=" + id_user +
                ", creation_date=" + creation_date +
                ", completion_date=" + completion_date +
                ", upload_date=" + upload_date +
                ", scheduled_date=" + scheduled_date +
                ", status=" + status +
                ", eventuid="+eventuid+
                '}';
    }

}
