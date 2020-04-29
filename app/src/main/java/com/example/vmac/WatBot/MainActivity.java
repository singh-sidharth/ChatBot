package com.example.vmac.WatBot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.DialogSuggestion;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


  private RecyclerView recyclerView;
  private ChatAdapter mAdapter;
  private ArrayList messageArrayList;
  private EditText inputMessage;
  private ImageButton btnSend;
  private boolean initialRequest;
  private static String TAG = "MainActivityBOT";
  private Context mContext;
  private Assistant watsonAssistant;
  private Response<SessionResponse> watsonAssistantSession;

  private void createServices() {
    watsonAssistant = new Assistant("2019-02-28", new IamAuthenticator(mContext.getString(R.string.assistant_apikey)));
    watsonAssistant.setServiceUrl(mContext.getString(R.string.assistant_url));   
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mContext = getApplicationContext();

    inputMessage = findViewById(R.id.message);
    btnSend = findViewById(R.id.btn_send);
    String customFont = "Montserrat-Regular.ttf";
    Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
    inputMessage.setTypeface(typeface);
    recyclerView = findViewById(R.id.recycler_view);

    messageArrayList = new ArrayList<>();
    mAdapter = new ChatAdapter(messageArrayList);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(mAdapter);
    this.inputMessage.setText("");
    this.initialRequest = true;

    btnSend.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (checkInternetConnection()) {
                sendMessage();
            }
        }
    });

    createServices();
    sendMessage();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.refresh:
            finish();
            startActivity(getIntent());

    }
        return(super.onOptionsItemSelected(item));
    }

    // Sending a message to Watson Assistant Service
    private void sendMessage() {

                        final String inputmessage = this.inputMessage.getText().toString().trim();
                        if (!this.initialRequest) {
                            Message inputMessage = new Message();
                            inputMessage.setMessage(inputmessage);
                            inputMessage.setId("1");
                            messageArrayList.add(inputMessage);
                        } else {
                            Message inputMessage = new Message();
                            inputMessage.setMessage(inputmessage);
                            inputMessage.setId("100");
                            this.initialRequest = false;

                        }

                        this.inputMessage.setText("");
                        mAdapter.notifyDataSetChanged();

                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    if (watsonAssistantSession == null) {
                                        ServiceCall<SessionResponse> call = watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.assistant_id)).build());
                                        watsonAssistantSession = call.execute();
                                    }

                                    MessageInput input = new MessageInput.Builder()
                                            .text(inputmessage)
                                            .build();
                                    MessageOptions options = new MessageOptions.Builder()
                                            .assistantId(mContext.getString(R.string.assistant_id))
                                            .input(input)
                                            .sessionId(watsonAssistantSession.getResult().getSessionId())
                                            .build();
                                    Response<MessageResponse> response = watsonAssistant.message(options).execute();
                                    Log.i(TAG, "run: " + response.getResult());
                                    if (response != null &&
                                            response.getResult().getOutput() != null &&
                                            !response.getResult().getOutput().getGeneric().isEmpty()) {

                                        List<RuntimeResponseGeneric> responses = response.getResult().getOutput().getGeneric();

                                        for (RuntimeResponseGeneric r : responses) {
                                            Message outMessage;
                                            switch (r.responseType()) {
                                                case "text":
                                                    outMessage = new Message();
                                                    outMessage.setMessage(r.text());
                                                    outMessage.setId("2");

                                                    messageArrayList.add(outMessage);
                                                    break;

                                                case "option":
                                                    outMessage =new Message();
                                                    String title = r.title();
                                                    String OptionsOutput = "";
                                                    for (int i = 0; i < r.options().size(); i++) {
                                                        DialogNodeOutputOptionsElement option = r.options().get(i);
                                                        OptionsOutput = OptionsOutput + option.getLabel() +"\n";

                                                    }
                                                    outMessage.setMessage(title + "\n" + OptionsOutput);
                                                    outMessage.setId("2");

                                                    messageArrayList.add(outMessage);
                                                    break;

                                                case "image":
                                                    outMessage = new Message(r);
                                                    messageArrayList.add(outMessage);
                                                    break;

                                                case "suggestion":
                                                    outMessage =new Message();
                                                    String SuggestionsOutput = "";
                                                    for (int i=0; i<r.suggestions().size(); i++){
                                                        DialogSuggestion suggestion=r.suggestions().get(i);
                                                        SuggestionsOutput = SuggestionsOutput + suggestion.getLabel()+"\n";
                                                    }
                                                    outMessage.setMessage("Did you mean : \n" + SuggestionsOutput);
                                                    outMessage.setId("2");

                                                    messageArrayList.add(outMessage);
                                                    break;
                                                default:
                                                    Log.e("Error", "Unhandled message type");
                                            }
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                mAdapter.notifyDataSetChanged();
                                                if (mAdapter.getItemCount() > 1) {
                                                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);

                                                }

                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        thread.start();
                    }

}



