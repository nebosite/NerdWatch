//-----------------------------------------------------------------------------
// NERD WATCH!
// written by Eric Jorgensen
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
    
    var sensor = null;
    var MAX_SIGNAL_STRENGTH = 65535;

    //-----------------------------------------------------------------------------
    // setColors
    //-----------------------------------------------------------------------------
    function debugPrint(text)
    {
        var debugElement = document.getElementById("debug-text");
        debugElement.innerHTML = text + "<br>" + debugElement.innerHTML;  
        if(debugElement.innerHTML.length > 1000) {
            debugElement.innerHTML = debugElement.innerHTML.substr(0,500);
        }
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
        if (!sensor) {
            return;
        }

        sensor.start(
            function onSensorStart() {
                sensor.getLightSensorData(
                    lightSensorCallback,
                    function onError(err) {
                        debugPrint("Sensor Error: " + err.message);
                        console.error('Getting light sensor data failed.',
                            err.message);
                    }
                );
                sensor.stop();
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
        var datetime = tizen.time.getCurrentDateTime(),
            nextInterval,
            strDay = document.getElementById("str-day"),
            strFullDate,
            getDay = datetime.getDay(),
            getDate = datetime.getDate(),
            getMonth = datetime.getMonth();

        // Check the update condition.
        // if prevDate is '0', it will always update the date.
        if (prevDay !== null) {
            if (prevDay === getDay) { // WHoops, try again!
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

        strFullDate = arrMonth[getMonth] + " " + getDate;
        strDay.innerHTML = strFullDate;

        // If an updateDate timer already exists, clear the previous timer.
        if (timerUpdateDate) {
            clearTimeout(timerUpdateDate);
        }

        // Set next timeout for date update.
        timerUpdateDate = setTimeout(function() {
            updateDate(getDay);
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

    //-----------------------------------------------------------------------------
    // Initialize everything and go!
    //-----------------------------------------------------------------------------
    function init() {
        document.getElementById("digital-body").style.backgroundImage = BACKGROUND_URL;
        updateDate(0);
        try {
            sensor = tizen.sensorservice.getDefaultSensor('LIGHT');
        } catch (err) {
            console.error('Could not access light sensor.',
                err.message);
        }

        bindEvents();
    }

    window.onload = init();
}());
