var SpeechRecognition = SpeechRecognition || webkitSpeechRecognition;
var recognition = new SpeechRecognition();
var finalTranscript = "";
var interimTranscript = "";
var recognizing = false;
var cancel = false;

recognition.continuous = true;
recognition.lang = "en-US";
recognition.interimResults = true;
recognition.maxAlternatives = 1;

recognition.onstart = function() {};
recognition.onresult = function(e) {
  e.results.forEach(result => {
    if (result.isFinal) {
      finalTranscript = finalTranscript + result[0].transcript;
    } else {
      interimTranscript = interimTranscript + result[0].transcript;
    }
  });

  finalTranscript = capitalize(finalTranscript);
};

recognition.onend = function() {
  console.log(finalTranscript.toLowerCase());
};

recognition.onerror = function(e) {
  console.log("error", e);
};

function startListening() {
  if (!recognizing) {
    recognition.start();
    finalTranscript = interimTranscript = "";
    recognizing = true;
  }
}

function endListening() {
  if (recognizing) {
    recognizing = false;
    recognition.stop();
  }
}

var liveSource;
var analyser;
var frequencyData;
var scaling = 1.5;

function update() {
  requestAnimationFrame(update);

  if (recognizing) {
    console.log(analyser.getByteFrequencyData(frequencyData));
  } else {
    console.log("completed");
  }
}

// creates an audiocontext and hooks up the audio input
var context = new AudioContext();
navigator.webkitGetUserMedia(
  { audio: true },
  function(stream) {
    console.log("Connected live audio input");
    if (!analyser) {
      liveSource = context.createMediaStreamSource(stream);
      // Create the analyser
      analyser = context.createAnalyser();
      analyser.smoothingTimeConstant = 0.3;
      analyser.fftSize = 64;
      frequencyData = new Uint8Array(analyser.frequencyBinCount);
      liveSource.connect(analyser);
    }

    update();
  },
  function() {
    console.log("Error connecting to audio");
  }
);

/// ##### BASIC UTILS #####
function capitalize(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}
