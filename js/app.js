//-----------------------------------------------------------------------------
// NERD WATCH!
// written by Eric Jorgensen
//-----------------------------------------------------------------------------

const METERS_PER_MILE = 1609.344;

//-----------------------------------------------------------------------------
// Random number helper
//-----------------------------------------------------------------------------
function rnd(max) {
    return Math.floor(Math.random() * max);
}

//-----------------------------------------------------------------------------
// Object Helper
//-----------------------------------------------------------------------------
function clone(thing) {
    var output = {}
    for(let propertyName in thing) {
        output[propertyName] = thing[propertyName];
    }
    return output;
}

//-----------------------------------------------------------------------------
// pad string on the left
//-----------------------------------------------------------------------------
function padLeft(text, width, padChar) {
    padChar = padChar || '0';
    text = text + '';
    return text.length >= width ? text : new Array(width - text.length + 1).join(padChar) + text;
}

console.log("Hi there")
if(typeof tizen !== 'undefined') {
    console.log("Tizen is defined: " + tizen)
}
else {
    console.log("Setting to fake tizen")
    var hour = 8;
    var minute = 8;
    var tizen = {
        time: {
            getCurrentDateTime: function () { 
                hour++;
                minute++;
                if(hour > 15) hour = 8;
                if(minute > 22) minute = 8;
                return new Date(`2020-12-17 ${hour}:${minute}`);
            },
            setTimezoneChangeListener: function (callMe) {}
        },
        sensorservice: {
            getDefaultSensor: function (sensorName) {
                switch(sensorName) {
                    case "LIGHT": return { start: function () {}}
                }
            }
        },
        ppm : {
            requestPermission(permissionId ,onHealthInfoSucceeded, onHealthInfoError) {
                console.debug(`Requested Permission for ${permissionId}`)
                onHealthInfoSucceeded();
            }
        },
        humanactivitymonitor : {
            start(name, callMe, onError) {
                switch(name) {
                    case "PEDOMETER" : 
                        console.log("Setting Fake Pedometer")
                        var pedData = {
                            stepStatus: "NOT_MOVING",
                            cumulativeTotalStepCount: 2,
                            cumulativeWalkStepCount: 2,
                            cumulativeRunStepCount: 2,
                            cumulativeCalorie: 2,
                            cumulativeDistance: 2,   
                        }
                        var currentCount = 0;
                        setInterval(() => {
                            currentCount--;
                            if(currentCount < 0) {
                                currentCount = rnd(5) + 3;
                                pedData.stepStatus = ["NOT_MOVING", "WALKING", "RUNNING"][rnd(3)];
                            }
                            pedData.cumulativeWalkStepCount += 5 + Math.floor(pedData.cumulativeWalkStepCount * .123231)
                            pedData.cumulativeRunStepCount += 5 + Math.floor(pedData.cumulativeWalkStepCount * .143443)
                            pedData.cumulativeTotalStepCount = pedData.cumulativeWalkStepCount + pedData.cumulativeRunStepCount
                            if(pedData.cumulativeTotalStepCount > 20000) {
                                pedData.cumulativeWalkStepCount = 0;
                                pedData.cumulativeRunStepCount = 0;
                                pedData.cumulativeTotalStepCount  =0;
                            }
                            pedData.cumulativeDistance += pedData.cumulativeTotalStepCount * .2;
                            pedData.cumulativeCalorie = pedData.cumulativeTotalStepCount * .0334234;
                            
                            callMe(clone(pedData))
                        },1000)
                        break;
                }
            }
        }
    }
}

//-----------------------------------------------------------------------------
// Main Functionality here
//-----------------------------------------------------------------------------
(function() {
	const DEFAULT_CHRONO_TEXT = "0:00:00.00";
    var timerUpdateDate = 0;
    var stopwatchStart = 0;
    var isStopwatchActive = false;
    var elapsedStopwatchTime = 0;
    var isTickVisible = false;
    var battery = navigator.battery || navigator.webkitBattery || navigator.mozBattery;
    var timeUpdateInterval;
    var stopwatchUpdateInterval;
    var BACKGROUND_URL = "url('./images/bg.jpg')";
    var arrMonth = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    var lastStopwatchClickTime = 0;
    var nightlightOn = false;
    var lightSensorValue = 0;

    if(typeof battery === 'undefined') {
        battery =  {
            _foo: "Hi",
            addEventListener: function (name, callMe) {
                console.log("Adding listener: " + name)
            }
        };
    }
    console.log("BATTERY: " + JSON.stringify(battery))
    var lightSensor = null;
    var MAX_SIGNAL_STRENGTH = 65535;

    //-----------------------------------------------------------------------------
    // setColors
    //-----------------------------------------------------------------------------
    function debugPrint(text)
    {
        var debugElement = document.getElementById("debug-text");
        debugElement.innerHTML = text.replace(/\n/g, "<br>");
        // debugElement.innerHTML = text + "<br>" + debugElement.innerHTML;  
        // if(debugElement.innerHTML.length > 1000) {
        //     debugElement.innerHTML = debugElement.innerHTML.substr(0,500);
        // }
    }
    
    //-----------------------------------------------------------------------------
    // setColors
    //-----------------------------------------------------------------------------
    function setColors() {
        var body = document.getElementById("digital-body");
        var battery = document.getElementById("battery-fill");

        var color = "white";
        var batteryColor = "#00a6ff";
        if(nightlightOn) {
            color = "gray";
        }
        else if(lightSensorValue == 0) {
            color = "red";
            batteryColor = "#FF4040";
        }

        body.style.color = color;
        body.style.borderColor = color;   
        battery.style.backgroundColor = batteryColor;     
    }

    //-----------------------------------------------------------------------------
    // getSensorValue
    //-----------------------------------------------------------------------------
    function getSensorValue(lightSensorCallback) {
        if (!lightSensor) {
            return;
        }

        lightSensor.start(
            function onSensorStart() {
                lightSensor.getLightSensorData(
                    lightSensorCallback,
                    function onError(err) {
                        debugPrint("Sensor Error: " + err.message);
                        console.error('Getting light sensor data failed.',
                            err.message);
                    }
                );
                lightSensor.stop();
            },
            function onError(err) {
                debugPrint("Couln't start light sensor: " + err.message);
                console.error('Could not start light sensor.',
                    err.message);
            }
        );
    }
    
    //-----------------------------------------------------------------------------
    // updateDate
    //-----------------------------------------------------------------------------
    function updateDate(prevDay) {
        var datetime = tizen.time.getCurrentDateTime();
        var nextInterval;
        var strDay = document.getElementById("str-day");
        var strFullDate;
        var day = datetime.getDay();
        var theDate = datetime.getDate();
        var month = datetime.getMonth();

        // Check the update condition.
        // if prevDate is '0', it will always update the date.
        if (prevDay !== null) {
            if (prevDay === day) { // WHoops, try again!
                nextInterval = 1000;
            } 
            else {
                // Calculate how much time is left until the next day.
                nextInterval =
                    (23 - datetime.getHours()) * 60 * 60 * 1000 +
                    (59 - datetime.getMinutes()) * 60 * 1000 +
                    (59 - datetime.getSeconds()) * 1000 +
                    (1000 - datetime.getMilliseconds()) +
                    1;
            }
        }

        strFullDate = arrMonth[month] + " " + theDate;
        strDay.innerHTML = strFullDate;

        // If an updateDate timer already exists, clear the previous timer.
        if (timerUpdateDate) {
            clearTimeout(timerUpdateDate);
        }

        debugPrint(
        `Heart rate: _TODO_ bpm`
        + `\nLight: _TODO_, UV: _TODO_`
        + `\nAcc: Linear _TODO_, Gyro _TODO_, Grav _TODO_`
        + `\nMagnet: _TODO_`
        + `\nPressure: _TODO_, Altitude _TODO_`
        + `\nTemperature: _TODO_`
        + `\nProximity: _TODO_`
        + `\nAvailable: _TODO_`
        + `\nBattery: _TODO_`
        )

        // Set next timeout for date update.
        timerUpdateDate = setTimeout(function() {
            updateDate(day);
        }, nextInterval);
    }

    countt = 0;
    //-----------------------------------------------------------------------------
    // updateTime
    //-----------------------------------------------------------------------------
    function updateTime() {
        var strHours = document.getElementById("str-hours"),
            strConsole = document.getElementById("str-console"),
            strMinutes = document.getElementById("str-minutes"),
            strAmpm = document.getElementById("str-ampm"),
            datetime = tizen.time.getCurrentDateTime(),
            hour = datetime.getHours(),
            minute = datetime.getMinutes();

        getSensorValue(function(data){
            lightSensorValue = data.lightLevel;
        })
        
        if (hour < 12) {
            strAmpm.innerHTML = "A";
        } else {
        	hour -= 12;
            strAmpm.innerHTML = "P";
        }
        
        if(hour === 0) hour = "12";
        else if (hour < 10) hour = "&nbsp;" + hour;
        
        if (minute < 10) minute = "0" + minute;

        strHours.innerHTML = hour;
        strMinutes.innerHTML = minute;

        // Each 0.5 second the visibility of flagConsole is changed.
        strConsole.style.visibility = isTickVisible ? "visible" : "hidden";
        isTickVisible = !isTickVisible;

        setColors();
    }
        
    //-----------------------------------------------------------------------------
    // updateStopWatch
    //-----------------------------------------------------------------------------
    function updateStopWatch() {
        var stopwatchText = document.getElementById("str-elapsedtime");
        var stopwatchTime = new Date().getTime();
        if(isStopwatchActive) {
        	elapsedStopwatchTime += (stopwatchTime - stopwatchStart);
        	stopwatchStart = stopwatchTime;        	
        }
        
        if(elapsedStopwatchTime === 0){
        	stopwatchText.innerHTML = DEFAULT_CHRONO_TEXT;
        }
        else {
        	var milliseconds = elapsedStopwatchTime % 1000;
        	var timeBuffer = Math.floor(elapsedStopwatchTime / 1000);
        	var hundredths = Math.floor(milliseconds / 10);
            var seconds = timeBuffer % 60;
        	timeBuffer = Math.floor(timeBuffer / 60);
        	var minutes = timeBuffer % 60;
        	timeBuffer = Math.floor(timeBuffer / 60);
            var hours = timeBuffer;
            
            if(minutes < 10) minutes = "0" + minutes;
            if(seconds < 10) seconds = "0" + seconds;
        	if(hundredths < 10) hundredths = "0" + hundredths;

        	
        	stopwatchText.innerHTML = hours + ":" + minutes + ":" + seconds + "." + hundredths;
        }
        
    }

    //-----------------------------------------------------------------------------
    // handleStopwatchClick
    //-----------------------------------------------------------------------------
    function handleStopwatchClick() {
        stopwatchStart = new Date().getTime();

        var doubleClicked = (stopwatchStart - lastStopwatchClickTime) < 200;
        lastStopwatchClickTime = stopwatchStart;
        
        if(doubleClicked)
        {
            elapsedStopwatchTime= 0; 
            if(isStopwatchActive)
            {
                isStopwatchActive = false;
                clearInterval(stopwatchUpdateInterval);
            }
            var stopwatchText = document.getElementById("str-elapsedtime");
            stopwatchText.innerHTML = DEFAULT_CHRONO_TEXT;
        }
        else {
            isStopwatchActive = !isStopwatchActive;
            if(isStopwatchActive) {
                stopwatchUpdateInterval = setInterval(updateStopWatch, 10);
            }
            else {
                clearInterval(stopwatchUpdateInterval);
            }
        }

        if(isStopwatchActive) {
        	// Keep the screen on all while the stopwatch is running
        	tizen.power.request("SCREEN", "SCREEN_NORMAL");
        }
        else {
        	tizen.power.release("SCREEN");
        }

    }


    //-----------------------------------------------------------------------------
    // handleNightlightClick
    //-----------------------------------------------------------------------------
    function handleNightlightClick() {
        nightlightOn = !nightlightOn;
        console.log("Clicky " + nightlightOn)
        var glow = document.getElementById("rec-glow");
        glow.style.visibility = nightlightOn ? "visible" : "hidden";
        if(nightlightOn) {
        	// Keep the screen on all the time when the light is on
        	tizen.power.request("SCREEN", "SCREEN_NORMAL");
        }
        else {
        	tizen.power.release("SCREEN");
        }
        setColors();
    }

    //-----------------------------------------------------------------------------
    // Get Battery State
    //-----------------------------------------------------------------------------
    function updateBatteryState() {
        var batteryLevel = Math.floor(battery.level * 100),
            batteryFill = document.getElementById("battery-fill");

        console.log("Battery: " + batteryLevel);
        batteryLevel = batteryLevel + 1;
        batteryFill.style.width = batteryLevel + "%";
    }

    //-----------------------------------------------------------------------------
    // Update all the information right now
    //-----------------------------------------------------------------------------
    function updateNow() {
        updateTime();
        updateDate(0);
        updateBatteryState();
    }

    var healthAvailable = false;
    //-----------------------------------------------------------------------------
    // Get steps information
    //-----------------------------------------------------------------------------
    function pingHeartRate()
    {
        if(healthAvailable) {
            tizen.humanactivitymonitor.start('HRM', function (hrmInfo) {
                console.log(`heart rate:` + hrmInfo.heartRate);
                tizen.humanactivitymonitor.stop('HRM');
            });            
        }
    }

    var stepsSnapshot = null;
    var lastStepsReading = null;
    var stepsSnapshotTime = 0;

    //-----------------------------------------------------------------------------
    // showStepData
    // data is a HumanActivityPedometerData object
    //-----------------------------------------------------------------------------
    function showStepData(data)
    {
        lastStepsReading = clone(data);
        var stepsWindow = document.getElementById("rec-steps");
        var miles = Math.floor(data.cumulativeDistance * 10.0 / METERS_PER_MILE)/10.0;
        var delta = null;
        if(stepsSnapshot) {
            delta = {};
            for(let propertyName in stepsSnapshot) {
                delta[propertyName] = lastStepsReading[propertyName] - stepsSnapshot[propertyName];
            }
        }

        var line1 = `🦶 ${data.cumulativeTotalStepCount}`;
        if(delta) line1 += ` +${delta.cumulativeTotalStepCount}`;
    
        var line2 = `${miles} mi`;
        if(delta) {
            var deltamiles = Math.floor(delta.cumulativeDistance * 10.0 / METERS_PER_MILE)/10.0;
            line2 += ` +${deltamiles}`;
        }

        var line3 = `cal: ${Math.floor(data.cumulativeCalorie)}`;
        if(delta) line3 += ` +${Math.floor(delta.cumulativeCalorie)}`;

        var line4 = "";
        if(delta) {
            console.log(stepsSnapshotTime)
            var seconds = Math.floor((Date.now() - stepsSnapshotTime)/1000.0);
            var minutes = Math.floor(seconds/60);
            seconds = seconds % 60;
            line4 = `${padLeft(minutes,2)}:${padLeft(seconds,2)}`
        }

        stepsWindow.innerHTML = `${line1}<br>${line2}<br>${line3}<br>${line4}`
    }

    //-----------------------------------------------------------------------------
    // Generic error reporter
    //-----------------------------------------------------------------------------
    function reportError(err) {
        console.log("Error: " + err);
    }

    //-----------------------------------------------------------------------------
    // Initialize everything and go!
    //-----------------------------------------------------------------------------
    function init() {
        document.getElementById("digital-body").style.backgroundImage = BACKGROUND_URL;
        updateDate(0);

        // Set up objects
        try {
            lightSensor = tizen.sensorservice.getDefaultSensor('LIGHT');
        } catch (err) {
            console.error('Could not access light sensor.',
                err.message);
        }

        // Sign up to read services
        // These should be listed in config.xml
        tizen.ppm.requestPermission(
            "http://tizen.org/privilege/healthinfo",
            () => {
                healthAvailable = true
                tizen.humanactivitymonitor.start("PEDOMETER", showStepData, reportError); 
            }, 
            (e) => {
                console.log("Healthinfo error " + JSON.stringify(e))
            });

        bindEvents();
    }

    //-----------------------------------------------------------------------------
    // Bind Events
    //-----------------------------------------------------------------------------
    function bindEvents() {
    	
        // Clock updates every 500 ms
        timeUpdateInterval = setInterval(updateTime, 500);

        // Clickable stopwatch
        var stopwatch = document.getElementById("rec-stopwatch");
        var stopwatchText = document.getElementById("str-elapsedtime");
        stopwatchText.innerHTML = DEFAULT_CHRONO_TEXT;
        stopwatch.addEventListener("click", handleStopwatchClick);

        // Clickable steps
        var stepsRec = document.getElementById("rec-steps");
        stepsRec.addEventListener("click", () => {
            stepsSnapshot = lastStepsReading;
            stepsSnapshotTime = Date.now();
        });

        // Night Light
        var lightButton = document.getElementById("button-flashlight");
        lightButton.addEventListener("click", handleNightlightClick);

        // battery state
        battery.addEventListener("chargingchange", updateBatteryState);
        battery.addEventListener("chargingtimechange", updateBatteryState);
        battery.addEventListener("dischargingtimechange", updateBatteryState);
        battery.addEventListener("levelchange", updateBatteryState);

        // ambientmodechanged
        window.addEventListener("ambientmodechanged", function(e) {
            console.log("Ambient mode is "  + (e.detail.ambientMode ? "ON" :"OFF"))
        });

        // Update on wakeup
        document.addEventListener("visibilitychange", function() {
            if (!document.hidden) updateNow();
        });

        // update on timezone change
        tizen.time.setTimezoneChangeListener(function() {
            updateNow();
        });
    }

    window.onload = init();
}());
