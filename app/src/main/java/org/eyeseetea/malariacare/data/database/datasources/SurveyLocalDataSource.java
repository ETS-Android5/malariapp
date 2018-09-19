package org.eyeseetea.malariacare.data.database.datasources;

import android.util.Log;

import org.eyeseetea.malariacare.data.boundaries.ISurveyDataSource;
import org.eyeseetea.malariacare.data.database.mapper.SurveyDBMapper;
import org.eyeseetea.malariacare.data.database.model.OptionDB;
import org.eyeseetea.malariacare.data.database.model.OrgUnitDB;
import org.eyeseetea.malariacare.data.database.model.ProgramDB;
import org.eyeseetea.malariacare.data.database.model.QuestionDB;
import org.eyeseetea.malariacare.data.database.model.SurveyDB;
import org.eyeseetea.malariacare.data.database.model.UserDB;
import org.eyeseetea.malariacare.data.database.model.ValueDB;
import org.eyeseetea.malariacare.domain.entity.Survey;
import org.eyeseetea.malariacare.domain.usecase.pull.SurveyFilter;

import java.util.ArrayList;
import java.util.List;

public class SurveyLocalDataSource implements ISurveyDataSource {
    private final static String TAG = ".SurveyLocalDataSource";

    @Override
    public List<Survey> getSurveys(SurveyFilter filters) {
        SurveyDBMapper surveyDBMapper = new SurveyDBMapper(
                OrgUnitDB.list(), ProgramDB.getAllPrograms(), QuestionDB.list(),
                OptionDB.list(), UserDB.list());

        //On the future implement this method to retrieve surveys from db
        List<SurveyDB> surveyDBS = new ArrayList<>();
        if(filters.isFindLastSurveyByOrgUnitAndProgram()){
            if(filters.getProgramUId() != null && filters.getOrgUnitUId() !=null){
                SurveyDB surveyDB = SurveyDB.findPlannedByOrgUnitAndProgram(filters.getOrgUnitUId(),
                        filters.getProgramUId());
                if(surveyDB!=null) {
                    surveyDBS.add(surveyDB);
                }
            }
            else{
                List<SurveyDB> surveyList = SurveyDB.findLastSurveysByProgramAndOrgUnit();
                if(surveyList!=null && surveyList.size()>0) {
                    surveyDBS.addAll(surveyList);
                }
            }

        }
        if(surveyDBS.size()>0){
            return surveyDBMapper.mapPlanningDBs(surveyDBS);
        }
        return new ArrayList<>();
    }

    @Override
    public void Save(List<Survey> surveys) {
        SurveyDBMapper surveyDBMapper = new SurveyDBMapper(
                OrgUnitDB.list(), ProgramDB.getAllPrograms(), QuestionDB.list(),
                OptionDB.list(), UserDB.list());

        List<SurveyDB> surveysDB = surveyDBMapper.mapSurveys(surveys);

        for (SurveyDB surveyDB : surveysDB) {
            try {
                surveyDB.save();

                if (surveyDB.getScoreDB() != null) {
                    surveyDB.getScoreDB().setSurvey(surveyDB);
                    surveyDB.getScoreDB().save();
                }

                for (ValueDB valueDB : surveyDB.getValues()) {
                    valueDB.setSurvey(surveyDB);
                    valueDB.save();
                }
            } catch (Exception e) {
                Log.e(TAG, "An error occurred saving Survey " + surveyDB.getEventUid() + ":" +
                         e.getMessage());
            }


        }

    }
}
