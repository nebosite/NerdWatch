/*
 * NERD WATCH!
 */

(function() {
    var timerUpdateDate = 0,
        stopwatchStart = 0,
        isStopwatchActive = false,
        elapsedStopwatchTime = 0,
        isTickVisible = false,
        battery = navigator.battery || navigator.webkitBattery || navigator.mozBattery,
        timeUpdateInterval,
        stopwatchUpdateInterval,
        BACKGROUND_URL = "url('./images/bg.jpg')",
        arrMonth = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    	

    /**
     * Updates the date and sets refresh callback on the next day.
     * @private
     * @param {number} prevDay - date of the previous day
     */
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
            if (prevDay === getDay) {
                /**
                 * If the date was not changed (meaning that something went wrong),
                 * call updateDate again after a second.
                 */
                nextInterval = 1000;
            } else {
                /**
                 * If the day was changed,
                 * call updateDate at the beginning of the next day.
                 */
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

    /**
     * Updates the current time.
     * @private
     */
    function updateTime() {
        var strHours = document.getElementById("str-hours"),
            strConsole = document.getElementById("str-console"),
            strMinutes = document.getElementById("str-minutes"),
            strAmpm = document.getElementById("str-ampm"),
            strStopwatch = document.getElementById("str-elapsedtime"),
            datetime = tizen.time.getCurrentDateTime(),
            hour = datetime.getHours(),
            minute = datetime.getMinutes();

        strHours.innerHTML = hour;
        strMinutes.innerHTML = minute;

        if (hour < 12) {
            strAmpm.innerHTML = "A";

        } else {
        	hour -= 12;
            strAmpm.innerHTML = "P";
        }
        
        if(hour === 0)
        {
            strHours.innerHTML = "12";
        }
        else if (hour < 10) {
            strHours.innerHTML = "&nbsp;" + hour;
        }
        
        if (minute < 10) {
            strMinutes.innerHTML = "0" + minute;
        }

        // Each 0.5 second the visibility of flagConsole is changed.
        strConsole.style.visibility = isTickVisible ? "visible" : "hidden";
        isTickVisible = !isTickVisible;
    }
        
    function updateStopWatch() {
        var stopwatchText = document.getElementById("str-elapsedtime");
        var stopwatchTime = new Date().getTime();
        if(isStopwatchActive) {
        	elapsedStopwatchTime += (stopwatchTime - stopwatchStart);
        	stopwatchStart = stopwatchTime;        	
        }
        
        if(elapsedStopwatchTime === 0){
        	stopwatchText.innerHTML = "--:--:--.--";
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

    /**
     * Sets to background image as BACKGROUND_URL,
     * and starts timer for normal digital watch mode.
     * @private
     */
    function initDigitalWatch() {
        document.getElementById("digital-body").style.backgroundImage = BACKGROUND_URL;
        timeUpdateInterval = setInterval(updateTime, 500);
    }

    /**
     * Gets battery state.
     * Updates battery level.
     * @private
     */
    function getBatteryState() {
        var batteryLevel = Math.floor(battery.level * 100),
            batteryFill = document.getElementById("battery-fill");

        console.log("Battery: " + batteryLevel);
        batteryLevel = batteryLevel + 1;
        batteryFill.style.width = batteryLevel + "%";
    }

    /**
     * Updates watch screen. (time and date)
     * @private
     */
    function updateWatch() {
        updateTime();
        updateDate(0);
    }

    /**
     * Binds events.
     * @private
     */
    function bindEvents() {
        // Clickable stopwatch
        var stopwatch = document.getElementById("rec-stopwatch");
        var stopwatchText = document.getElementById("str-elapsedtime");
        stopwatchText.innerHTML = "-:--:--.--"
        var lastClickTime = 0;
        stopwatch.addEventListener("click", function() {
            stopwatchStart = new Date().getTime();
            if(stopwatchStart - lastClickTime < 200)
            {
                elapsedStopwatchTime= 0; 
                if(isStopwatchActive)
                {
                    isStopwatchActive = false;
                    clearInterval(stopwatchUpdateInterval);
                }
                stopwatchText.innerHTML = "-:--:--.--";
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
            lastClickTime = stopwatchStart;
        })
        
        // add eventListener for battery state
        battery.addEventListener("chargingchange", getBatteryState);
        battery.addEventListener("chargingtimechange", getBatteryState);
        battery.addEventListener("dischargingtimechange", getBatteryState);
        battery.addEventListener("levelchange", getBatteryState);

        // add eventListener for timetick
        window.addEventListener("timetick", function() {
            ambientDigitalWatch();
        });

        // add eventListener for ambientmodechanged
        window.addEventListener("ambientmodechanged", function(e) {
            if (e.detail.ambientMode === true) {
                // rendering ambient mode case
                ambientDigitalWatch();

            } else {
                // rendering normal digital mode case
                initDigitalWatch();
            }
        });

        // add eventListener to update the screen immediately when the device wakes up.
        document.addEventListener("visibilitychange", function() {
            if (!document.hidden) {
                updateWatch();
            }
        });

        // add event listeners to update watch screen when the time zone is changed.
        tizen.time.setTimezoneChangeListener(function() {
            updateWatch();
        });

    }

    /**
     * Initializes date and time.
     * Sets to digital mode.
     * @private
     */
    function init() {
        initDigitalWatch();
        updateDate(0);

        bindEvents();
    }

    window.onload = init();
}());
