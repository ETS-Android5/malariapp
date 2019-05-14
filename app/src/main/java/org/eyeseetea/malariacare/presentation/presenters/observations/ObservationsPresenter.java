package org.eyeseetea.malariacare.presentation.presenters.observations;

import static org.eyeseetea.malariacare.domain.entity.ObservationStatus.*;

import android.content.Context;
import android.support.annotation.NonNull;

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.data.database.model.CompositeScoreDB;
import org.eyeseetea.malariacare.data.database.model.QuestionDB;
import org.eyeseetea.malariacare.data.database.model.SurveyDB;
import org.eyeseetea.malariacare.data.database.utils.planning.SurveyPlanner;
import org.eyeseetea.malariacare.domain.entity.CompetencyScoreClassification;
import org.eyeseetea.malariacare.domain.entity.Observation;
import org.eyeseetea.malariacare.domain.entity.ObservationStatus;
import org.eyeseetea.malariacare.domain.entity.ServerMetadata;
import org.eyeseetea.malariacare.domain.usecase.GetObservationBySurveyUidUseCase;
import org.eyeseetea.malariacare.domain.usecase.GetServerMetadataUseCase;
import org.eyeseetea.malariacare.domain.usecase.SaveObservationUseCase;
import org.eyeseetea.malariacare.observables.ObservablePush;
import org.eyeseetea.malariacare.presentation.mapper.observations.MissedStepMapper;
import org.eyeseetea.malariacare.presentation.mapper.observations.ObservationMapper;
import org.eyeseetea.malariacare.presentation.viewmodels.observations.ActionViewModel;
import org.eyeseetea.malariacare.presentation.viewmodels.observations.MissedStepViewModel;
import org.eyeseetea.malariacare.presentation.viewmodels.observations.ObservationViewModel;
import org.eyeseetea.malariacare.utils.DateParser;
import org.eyeseetea.malariacare.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ObservationsPresenter {
    private final GetObservationBySurveyUidUseCase mGetObservationBySurveyUidUseCase;
    private final GetServerMetadataUseCase mGetServerMetadataUseCase;
    private final SaveObservationUseCase mSaveObservationUseCase;

    private final Context mContext;
    private View mView;
    private SurveyDB mSurvey;

    private ServerMetadata mServerMetadata;
    private String mSurveyUid;

    private ObservationViewModel mObservationViewModel;

    private List<MissedStepViewModel> missedCriticalSteps;
    private List<MissedStepViewModel> missedNonCriticalSteps;

    public ObservationsPresenter(Context context,
            GetObservationBySurveyUidUseCase getObservationBySurveyUidUseCase,
            GetServerMetadataUseCase getServerMetadataUseCase,
            SaveObservationUseCase saveObservationUseCase) {
        this.mContext = context;
        this.mGetObservationBySurveyUidUseCase = getObservationBySurveyUidUseCase;
        this.mGetServerMetadataUseCase = getServerMetadataUseCase;
        this.mSaveObservationUseCase = saveObservationUseCase;

        ObservablePush.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                if (mView != null) {
                    refreshObservation();
                }
            }
        });
    }

    public void attachView(View view, String surveyUid) {
        this.mView = view;
        this.mSurveyUid = surveyUid;


        LoadData();
    }

    public void detachView() {
        this.mView = null;
    }

    private void LoadData() {
        mGetServerMetadataUseCase.execute(new GetServerMetadataUseCase.Callback() {
            @Override
            public void onSuccess(ServerMetadata serverMetadata) {
                ObservationsPresenter.this.mServerMetadata = serverMetadata;
                loadObservation();
            }

            @Override
            public void onError(Exception e) {
                System.out.println(
                        "An error has occur retrieving server metadata: " + e.getMessage());
            }
        });
    }

    private void loadObservation() {
        mGetObservationBySurveyUidUseCase.execute(mSurveyUid,
                new GetObservationBySurveyUidUseCase.Callback() {
                    @Override
                    public void onSuccess(Observation observation) {
                        mObservationViewModel =
                                ObservationMapper.mapToViewModel(observation, mServerMetadata);


                        loadSurvey();
                        loadMissedSteps();
                        updateStatus();
                        showObservation();
                    }

                    @Override
                    public void onObservationNotFound() {
                        mObservationViewModel = new ObservationViewModel(mSurveyUid);
                        saveObservation();

                        loadSurvey();
                        loadMissedSteps();
                        updateStatus();
                        showObservation();
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println(
                                "An error has occur retrieving observation: " + e.getMessage());
                    }
                });
    }


    private void loadSurvey() {
        mSurvey = SurveyDB.getSurveyByUId(mSurveyUid);

        if (mView != null) {
            DateParser dateParser = new DateParser();
            String formattedCompletionDate = "NaN";
            if (mSurvey.getCompletionDate() != null) {
                formattedCompletionDate = dateParser.format(mSurvey.getCompletionDate(),
                        DateParser.EUROPEAN_DATE_FORMAT);
            }

            String formattedNextDate = "NaN";
            if (mSurvey != null) {
                formattedNextDate = dateParser.format(
                        SurveyPlanner.getInstance().findScheduledDateBySurvey(mSurvey),
                        DateParser.EUROPEAN_DATE_FORMAT);
            }

            CompetencyScoreClassification classification =
                    CompetencyScoreClassification.get(
                            mSurvey.getCompetencyScoreClassification());

            mView.renderHeaderInfo(mSurvey.getOrgUnit().getName(), mSurvey.getMainScore(),
                    formattedCompletionDate, formattedNextDate, classification);
        }
    }

    private void loadMissedSteps() {
        List<QuestionDB> criticalQuestions = QuestionDB.getFailedQuestions(
                mSurvey.getId_survey(), true);

        List<CompositeScoreDB> compositeScoresOfCriticalFailedQuestions =
                getValidTreeOfCompositeScores(true);

        missedCriticalSteps = MissedStepMapper.mapToViewModel(criticalQuestions,
                compositeScoresOfCriticalFailedQuestions);

        List<QuestionDB> nonCriticalQuestions = QuestionDB.getFailedQuestions(
                mSurvey.getId_survey(), false);

        List<CompositeScoreDB> compositeScoresOfNonCriticalFailedQuestions =
                getValidTreeOfCompositeScores(false);

        missedNonCriticalSteps = MissedStepMapper.mapToViewModel(nonCriticalQuestions,
                compositeScoresOfNonCriticalFailedQuestions);
    }


    private void showObservation() {
        if (mView != null) {
            mView.renderMissedCriticalSteps(missedCriticalSteps);
            mView.renderMissedNonCriticalSteps(missedNonCriticalSteps);
            mView.renderProvider(mObservationViewModel.getProvider());

            mView.renderAction1(mObservationViewModel.getAction1());
            mView.renderAction2(mObservationViewModel.getAction2());
            mView.renderAction3(mObservationViewModel.getAction3());
        }
    }

    public void providerChanged(String provider) {
        if (!provider.equals(mObservationViewModel.getProvider())) {
            mObservationViewModel.setProvider(provider);
            saveObservation();
        }
    }

    private void saveObservation() {
        Observation observation =
                ObservationMapper.mapToObservation(mObservationViewModel, mServerMetadata);

        mSaveObservationUseCase.execute(observation, new SaveObservationUseCase.Callback() {
            @Override
            public void onSuccess() {
                System.out.println("Observation saved successfully");
            }

            @Override
            public void onError(Exception e) {
                System.out.println(
                        "An error has occur saving Observation: " + e.getMessage());
            }
        });
    }


    public void completeObservation() {
        mObservationViewModel.setStatus(COMPLETED);
        saveObservation();

        if (mView != null) {
            mView.changeToReadOnlyMode();

            updateStatus();
        }
    }

    private void updateStatus() {
        if (mView != null) {
            mView.updateStatusView(mObservationViewModel.getStatus());
        }


        switch (mObservationViewModel.getStatus()) {
            case COMPLETED:
            case SENT:
                mView.enableShareButton();
                mView.changeToReadOnlyMode();
                break;

            case IN_PROGRESS:
                mView.disableShareButton();
                break;
        }
    }

    public void shareObsActionPlan() {
        if (mView != null) {

            if (mSurvey.getStatus() != Constants.SURVEY_SENT) {
                mView.shareNotSent(mContext.getString(R.string.feedback_not_sent));
            } else {
                mView.shareByText(mObservationViewModel, mSurvey, missedCriticalSteps, missedNonCriticalSteps);
            }
        }
    }

    public void onAction1Changed(ActionViewModel actionViewModel) {
        mObservationViewModel.setAction1(actionViewModel);
        saveObservation();
    }

    public void onAction2Changed(ActionViewModel actionViewModel) {
         mObservationViewModel.setAction2(actionViewModel);
        saveObservation();
    }

    public void onAction3Changed(ActionViewModel actionViewModel) {
        mObservationViewModel.setAction3(actionViewModel);
        saveObservation();
    }
    @NonNull
    private List<CompositeScoreDB> getValidTreeOfCompositeScores(boolean critical) {
        List<CompositeScoreDB> compositeScoreList = QuestionDB.getCompositeScoreOfFailedQuestions(
                mSurvey.getId_survey(), critical);

        List<CompositeScoreDB> compositeScoresTree = new ArrayList<>();
        for (CompositeScoreDB compositeScore : compositeScoreList) {
            buildCompositeScoreTree(compositeScore, compositeScoresTree);
        }

        //Order composite scores
        Collections.sort(compositeScoresTree, (Comparator) (o1, o2) -> {

            CompositeScoreDB cs1 = (CompositeScoreDB) o1;
            CompositeScoreDB cs2 = (CompositeScoreDB) o2;

            return new Integer(cs1.getOrder_pos().compareTo(cs2.getOrder_pos()));
        });
        return compositeScoresTree;
    }

    //Recursive compositescore parent builder
    private void buildCompositeScoreTree(CompositeScoreDB compositeScore,
            List<CompositeScoreDB> compositeScoresTree) {
        if (compositeScore.getHierarchical_code().equals("0")) {
            //ignore composite score root
            return;
        }
        if (!compositeScoresTree.contains(compositeScore)) {
            compositeScoresTree.add(compositeScore);
        }
        if (compositeScore.hasParent()) {
            buildCompositeScoreTree(compositeScore.getComposite_score(), compositeScoresTree);
        }
    }

    private void refreshObservation() {
        mGetObservationBySurveyUidUseCase.execute(mSurveyUid,
                new GetObservationBySurveyUidUseCase.Callback() {
                    @Override
                    public void onSuccess(Observation observation) {
                        mObservationViewModel =
                                ObservationMapper.mapToViewModel(observation, mServerMetadata);

                        updateStatus();
                    }

                    @Override
                    public void onObservationNotFound() {
                        System.out.println(
                                "Observation not found by surveyUid: " + mSurveyUid);
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println(
                                "An error has occur retrieving observation: " + e.getMessage());
                    }
                });
    }



    public interface View {
        void changeToReadOnlyMode();

        void renderProvider(String provider);

        void renderMissedCriticalSteps(List<MissedStepViewModel> missedCriticalSteps);

        void renderMissedNonCriticalSteps(List<MissedStepViewModel> missedNonCriticalSteps);

        void renderHeaderInfo(String orgUnitName, Float mainScore, String completionDate,
                String nextDate, CompetencyScoreClassification classification);

        void updateStatusView(ObservationStatus status);

        void shareByText(ObservationViewModel observationViewModel, SurveyDB survey,
                List<MissedStepViewModel> missedCriticalStepViewModels,
                List<MissedStepViewModel> missedNonCriticalStepViewModels);

        void shareNotSent(String surveyNoSentMessage);

        void enableShareButton();

        void disableShareButton();

        void renderAction1(ActionViewModel action1);
        void renderAction2(ActionViewModel action2);
        void renderAction3(ActionViewModel action3);
    }
}