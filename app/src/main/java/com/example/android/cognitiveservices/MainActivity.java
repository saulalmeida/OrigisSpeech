package com.example.android.cognitiveservices;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents{

    int m_waitSeconds = 0;
    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    EditText _logText;
    RadioGroup _radioGroup;
    Button _buttonSelectMode;
    Button _startButton;

    public enum FinalResponseStatus {
        NotReceived,
        OK,
        Timeout
    }

    public String getPrimaryKey(){
        return this.getString(R.string.primaryKey);
    }

    private String getLuisAppId(){
        return this.getString(R.string.luisAppID);
    }

    private String getLuisSubscriptionID(){
        return this.getString(R.string.luisSubscriptionID);
    }

    private Boolean getUseMicrophone(){
        int id = this._radioGroup.getCheckedRadioButtonId();
        return id ==R.id.micIntentRadioButton||
                id== R.id.micDictationRadioButton;
    }

    private Boolean getWantIntent(){
        int id = this._radioGroup.getCheckedRadioButtonId();
        return id == R.id.dataShortIntentRadioButton||
                id== R.id.micIntentRadioButton;
    }

    private SpeechRecognitionMode getMode(){
        int id = this._radioGroup.getCheckedRadioButtonId();
        if (id== R.id.micDictationRadioButton ||
                id == R.id.dataLongRadioButton){
            return SpeechRecognitionMode.LongDictation;
        }

        return SpeechRecognitionMode.ShortPhrase;
    }

    private String getDefaultLocale(){
        return "es-es";
    }


    private String getShortWaveFile(){
        return "whatstheweatherlike.wav";
    }

    private String getLongWaveFile(){
        return "batman.wav";
    }

    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this._logText = (EditText) findViewById(R.id.editText1);
        this._radioGroup = (RadioGroup)findViewById(R.id.groupMode);
        this._buttonSelectMode = (Button)findViewById(R.id.buttonSelectMode);
        this._startButton = (Button)findViewById(R.id.button1);

        if (getString(R.string.primaryKey).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
        // setup the buttons
        final MainActivity This = this;
        this._startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click(arg0);
            }
        });

        this._buttonSelectMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.ShowMenu(This._radioGroup.getVisibility() == View.INVISIBLE);
            }
        });

        this._radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                This.RadioButton_Click(rGroup, checkedId);
            }
        });

        this.ShowMenu(true);
    }

    private void ShowMenu(boolean show) {
        if (show) {
            this._radioGroup.setVisibility(View.VISIBLE);
            this._logText.setVisibility(View.INVISIBLE);
        } else {
            this._radioGroup.setVisibility(View.INVISIBLE);
            this._logText.setText("");
            this._logText.setVisibility(View.VISIBLE);
        }
    }

    private void StartButton_Click(View arg0) {
        this._startButton.setEnabled(false);
        this._radioGroup.setEnabled(false);

        this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;

        this.ShowMenu(false);

        this.LogRecognitionStart();

        if (this.getUseMicrophone()) {
            if (this.micClient == null) {
                if (this.getWantIntent()) {
                    this.WriteLine("--- Start microphone dictation with Intent detection ----");

                    this.micClient =
                            SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                    this,
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimaryKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else
                {
                    this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                            this,
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey());
                }

                this.micClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            this.micClient.startMicAndRecognition();
        }
        else
        {
            if (null == this.dataClient) {
                if (this.getWantIntent()) {
                    this.dataClient =
                            SpeechRecognitionServiceFactory.createDataClientWithIntent(
                                    this,
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimaryKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else {
                    this.dataClient = SpeechRecognitionServiceFactory.createDataClient(
                            this,
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey());
                }

                this.dataClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            this.SendAudioHelper((this.getMode() == SpeechRecognitionMode.ShortPhrase) ? this.getShortWaveFile() : this.getLongWaveFile());
        }
    }

    private void LogRecognitionStart() {
        String recoSource;
        if (this.getUseMicrophone()) {
            recoSource = "microphone";
        } else if (this.getMode() == SpeechRecognitionMode.ShortPhrase) {
            recoSource = "short wav file";
        } else {
            recoSource = "long wav file";
        }

        this.WriteLine("\n--- Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }

    private void SendAudioHelper(String filename) {
        RecognitionTask doDataReco = new RecognitionTask(this.dataClient, this.getMode(), filename);
        try
        {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
        }
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone() && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDicationMessage) {
            this._startButton.setEnabled(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {
            this.WriteLine("********* Final n-BEST Results *********");
            for (int i = 0; i < response.Results.length; i++) {
                this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                        " Text=\"" + response.Results[i].DisplayText + "\"");
            }

            this.WriteLine();
        }
    }


    @Override
    public void onPartialResponseReceived(String response) {
        this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
        this.WriteLine(response);
        this.WriteLine();


    }

    public void onIntentReceived(final String payload) {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        this.WriteLine();
    }

    @Override
    public void onError(int errorCode, String response) {
        this._startButton.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();

    }

    @Override
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
        if (!recording) {
            this.micClient.endMicAndRecognition();
            this._startButton.setEnabled(true);
        }

    }

    private void WriteLine() {
        this.WriteLine("");
    }

    private void WriteLine(String text) {
        this._logText.append(text + "\n");
    }

    private void RadioButton_Click(RadioGroup rGroup, int checkedId) {
        // Reset everything
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
            try {
                this.micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.micClient = null;
        }

        if (this.dataClient != null) {
            try {
                this.dataClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.dataClient = null;
        }

        this.ShowMenu(false);
        this._startButton.setEnabled(true);
    }

    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = getAssets().open(filename);
                int bytesRead = 0;
                byte[] buffer = new byte[1024];

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service.
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            finally {
                dataClient.endAudio();
            }

            return null;
        }
    }

}
