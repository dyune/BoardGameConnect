/**
 * Formats an ISO date string or Date object into a readable date and time string.
 * **Forces the display timezone to America/New_York (EST/EDT).**
 * Includes timezone abbreviation in the time string.
 * 
 * @param {string|Date} dateTimeInput - The date/time to format.
 * @returns {{ date: string, time: string, tz: string }} - Object with formatted date, time strings, and the forced timezone.
 */
export const formatDateTimeForDisplay = (dateTimeInput) => {
  if (!dateTimeInput) return { date: "Date N/A", time: "Time N/A", tz: "N/A" };

  try {
    const dateObj = new Date(dateTimeInput); // Use the original input directly

    // Check for invalid date
    if (isNaN(dateObj.getTime())) {
      console.warn("[formatDateTimeForDisplay] Invalid date input:", dateTimeInput);
      return { date: "Invalid Date", time: "Invalid Time", tz: "Invalid" };
    }

    // --- Force Timezone to America/New_York ---
    const forcedTimezone = 'America/New_York'; 
    // --- End Force Timezone ---

    const dateOptions = { 
      month: "short", 
      day: "numeric", 
      year: "numeric",
      timeZone: forcedTimezone // Use the forced timezone
    };
    const timeOptions = { 
      hour: "2-digit", 
      minute: "2-digit", 
      timeZone: forcedTimezone, // Use the forced timezone
      timeZoneName: 'short' // Add timezone abbreviation (e.g., EDT, EST)
    };

    let formattedDate, formattedTime;

    try {
      // Use Intl.DateTimeFormat with the forced timezone
      // Pass empty array for locale to use browser default for language/date format
      formattedDate = new Intl.DateTimeFormat([], dateOptions).format(dateObj);
      formattedTime = new Intl.DateTimeFormat([], timeOptions).format(dateObj);
    } catch (intlError) {
      console.warn("[formatDateTimeForDisplay] Intl.DateTimeFormat failed, falling back.", intlError);
      // Fallback might not support timeZone option as reliably, but try
      try {
         // Pass empty array for locale to use browser default
         formattedDate = dateObj.toLocaleDateString([], dateOptions); 
         formattedTime = dateObj.toLocaleTimeString([], timeOptions); 
      } catch (fallbackError) {
         console.error("[formatDateTimeForDisplay] Fallback formatting with options failed:", fallbackError);
         // Final, simplest fallback if options cause errors
         formattedDate = dateObj.toLocaleDateString(); 
         formattedTime = dateObj.toLocaleTimeString([], { 
             hour: '2-digit', 
             minute: '2-digit', 
             timeZone: forcedTimezone, // Still try to apply timezone in simplest fallback
             timeZoneName: 'short' 
         }); 
      }
    }
    
    // Log the result for debugging
    console.log("[formatDateTimeForDisplay] Final Output (Forced TZ):", { 
        input: dateTimeInput, 
        interpretedUTC: dateObj.toISOString(), // Show the UTC interpretation
        forcedTimezone: forcedTimezone, 
        formattedDate, 
        formattedTime 
    });

    // Return including the forced timezone
    return { date: formattedDate, time: formattedTime, tz: forcedTimezone }; 
  } catch (error) {
    console.error("[formatDateTimeForDisplay] Error formatting date/time:", dateTimeInput, error);
    return { date: "Format Error", time: "Format Error", tz: "Error" };
  }
};

// Add other date utilities if needed...