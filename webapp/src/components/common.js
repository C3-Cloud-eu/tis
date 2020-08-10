
export function errorMsg(error) {
    let responseBody = error.response.data;
    if (responseBody) return responseBody instanceof Object ? JSON.stringify(responseBody) : responseBody;
    else if (error.response) return `Error: ${error.response.status} (${error.response.statusText})`;
    else return error;
};

export const RepeatOption = {
    "none"    : 'None',
    "1-hour"  : 'Every hour',
    "1-day"   : 'Every day',
    "1-week"  : 'Every week',
    "1-month" : 'Every month',
};