/*
 * Copyright (c) 2015.
 *
 * This file is part of Facility QA Tool App.
 *
 *  Facility QA Tool App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Facility QA Tool App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare.layout.adapters.survey;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.database.model.Header;
import org.eyeseetea.malariacare.database.model.Option;
import org.eyeseetea.malariacare.database.model.Question;
import org.eyeseetea.malariacare.database.model.Tab;
import org.eyeseetea.malariacare.database.utils.ReadWriteDB;
import org.eyeseetea.malariacare.layout.adapters.general.OptionArrayAdapter;
import org.eyeseetea.malariacare.layout.score.ScoreRegister;
import org.eyeseetea.malariacare.layout.utils.LayoutUtils;
import org.eyeseetea.malariacare.utils.Constants;
import org.eyeseetea.malariacare.views.CustomEditText;
import org.eyeseetea.malariacare.views.CustomTextView;

import java.util.List;

/**
 * Created by Jose on 11/04/2015.
 */
public class CustomIQTABAdapter extends ATabAdapter {

    int number_rows_section;

    static class ViewHolder {
        public CustomTextView number;
        public Spinner spinner;
        public CustomEditText parasites;
        public Spinner species;
    }

    public CustomIQTABAdapter(Tab tab, Context context) {
        super(tab, context, R.layout.form_custom);

        if (getItems().size()>0)
            number_rows_section = (int) ((Header) getItems().get(0)).getNumberOfQuestionParents() +1;

        for (int i = 0; i < 2 * number_rows_section; i++) {
            Object item = getItems().get(i);
            if (item instanceof Question)
                calculateMatch((Question) item);
        }

        for (int i = 2 * number_rows_section; i<getItems().size();i++) {
            Object item = getItems().get(i);
            if (item instanceof Question) {
                Question result = ((Question) item).getChildren().get(0);
                ScoreRegister.addRecord(result, ScoreRegister.calcNum(result), ScoreRegister.calcDenum(result));
            }
        }
    }

    /**
     * Factory method to build a CustomIQTABAdapter.
     * @param tab
     * @param context
     * @return
     */
    public static CustomIQTABAdapter build(Tab tab, Context context){
        return new CustomIQTABAdapter(tab, context);
    }

    @Override
    public Float getScore() {

        List<Float> numdenum = ScoreRegister.calculateGeneralScore(getTab());

        Float num = numdenum.get(0);
        Float denum = numdenum.get(1);

        return num/denum;

    }

    class Bool {
        public boolean value;

        public Bool(boolean value) {
            this.value = value;
        }
    }

    @Override
    public int getCount() {
        return 2*number_rows_section;
    }

    public void calculateMatch(Question question) {
        int simetric_position;
        int result_position;

        int position = getItems().indexOf(question);

        Question q1, q2;

        if (position > number_rows_section) {
            simetric_position = position - number_rows_section;
            result_position = position - number_rows_section - 1;
        } else {
            simetric_position = position + number_rows_section;
            result_position = position - 1;
        }

        q1 = ((Question) getItems().get(position)).getChildren().get(0);
        q2 = ((Question) getItems().get(simetric_position)).getChildren().get(0);

        Question questionAnswer =  (Question) getItems().get(2*number_rows_section+result_position+1);
        Question testResult = questionAnswer.getChildren().get(0);

        if (q1.getValueBySession() != null && q2.getValueBySession() != null &&
                q1.getValueBySession().getOption().equals(q2.getValueBySession().getOption())) {

            ReadWriteDB.saveValuesDDL(testResult,  testResult.getAnswer().getOptions().get(0));
            ScoreRegister.addRecord(testResult, ScoreRegister.calcNum(testResult), ScoreRegister.calcNum(testResult));

        } else {

            ScoreRegister.addRecord(testResult, 0F, ScoreRegister.calcDenum(testResult));

            if (q1.getValueBySession() != null && q2.getValueBySession() != null) {
                ReadWriteDB.saveValuesDDL(testResult,  testResult.getAnswer().getOptions().get(1));
            }
            else
                ReadWriteDB.deleteValue(testResult);
        }
    }

    private void setValues(ViewHolder viewHolder, Question question) {
        viewHolder.number.setText(question.getForm_name());
        viewHolder.species.setSelection(ReadWriteDB.readPositionOption(question.getChildren().get(2)));
        viewHolder.parasites.setText(ReadWriteDB.readValueQuestion(question.getChildren().get(1)));
        viewHolder.spinner.setSelection(ReadWriteDB.readPositionOption(question.getChildren().get(0)));
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = null;

        final Question parasites;
        final Question test;
        final Question species;
        final Question question;

        if (position < 2*number_rows_section) {

            final Object item = getItem(position);
            final ViewHolder viewHolder = new ViewHolder();

            if (item instanceof Header) {
                if (position == 0)
                    rowView = getInflater().inflate(R.layout.iqtabheader1, parent, false);
                else
                    rowView = getInflater().inflate(R.layout.iqtabheader2, parent, false);
            } else {

                question = (Question) item;

                rowView = getInflater().inflate(R.layout.iqatab_record, parent, false);
                viewHolder.number = (CustomTextView) rowView.findViewById(R.id.number);
                viewHolder.spinner = (Spinner) rowView.findViewById(R.id.testRes);
                viewHolder.parasites = (CustomEditText) rowView.findViewById(R.id.parasites);
                viewHolder.species = (Spinner) rowView.findViewById(R.id.species);

                List<Option> optionList = ((Question) item).getChildren().get(0).getAnswer().getOptions();
                optionList.add(0, new Option(Constants.DEFAULT_SELECT_OPTION));

                viewHolder.spinner.setAdapter(new OptionArrayAdapter(getContext(), optionList));

                optionList = ((Question) item).getChildren().get(2).getAnswer().getOptions();
                optionList.add(0, new Option(Constants.DEFAULT_SELECT_OPTION));

                viewHolder.species.setAdapter(new OptionArrayAdapter(getContext(), optionList));

                test = question.getChildren().get(0);
                parasites = question.getChildren().get(1);
                species = question.getChildren().get(2);

                rowView.setBackgroundResource(LayoutUtils.calculateBackgrounds(position));

                viewHolder.parasites.addTextChangedListener(new TextWatcher() {

                    Bool viewCreated = new Bool(false);

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (viewCreated.value) {
                            ReadWriteDB.saveValuesText(parasites, s.toString());
                        } else viewCreated.value = true;
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });


                viewHolder.species.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    Bool viewCreated = new Bool(false);

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        if (viewCreated.value) {
                            ReadWriteDB.saveValuesDDL(species, (Option) viewHolder.species.getItemAtPosition(pos));
                        } else viewCreated.value = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                viewHolder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    Bool viewCreated = new Bool(false);

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                        if (viewCreated.value) {
                            ReadWriteDB.saveValuesDDL(test, (Option) viewHolder.spinner.getItemAtPosition(pos));
                            calculateMatch(question);
                        } else viewCreated.value = true;

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                setValues(viewHolder, question);


            }

        }

        return rowView;
    }
}