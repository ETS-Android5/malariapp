package org.eyeseetea.malariacare.data.boundaries;

import org.eyeseetea.malariacare.domain.entity.IData;
import org.eyeseetea.malariacare.domain.entity.pushsummary.PushReport;
import org.eyeseetea.malariacare.domain.usecase.pull.SurveyFilter;

import java.util.List;
import java.util.Map;

public interface IDataRemoteDataSource {
    List<? extends IData> get(SurveyFilter filters) throws Exception;
    Map<String, PushReport> save (List<? extends IData> dataList) throws Exception;
}
