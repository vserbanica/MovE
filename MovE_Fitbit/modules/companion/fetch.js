//INFO: Not used any more

export default class messaging {
  //Fetch data from an API endpoint and return a promise
  async get(url) {
    console.log('Line 16: companion - fetch - get() URL' + url)
    const trimmedURL = url.replace(/ /g,"");
    console.log('Line 16: companion - fetch - get() trimmedURL' + trimmedURL)
    return await fetch(trimmedURL)
      .then(handleResponse)
      .then((data) => {
        logs.add(`Line 11: companion - fetch - get() Data Okay return`)
        return data;
      }).catch((error) => {
        console.log(`Line 14 ERROR companion - fetch - get() ${JSON.stringify(error)}`)
        
        return error;
      });
  };
};

function handleResponse (response) {
  
  let contentType = response.headers.get('content-type');
  let jsonContent = response.json();
  let responsContnent = response.ok;
  
  console.log(`Content type: ${contentType}`);
  console.log(`JSON: ${JSON.stringify(jsonContent)}`);
   console.log(`responsContnent: ${responsContnent}`);
  if (contentType.includes('application/json')) {
    return handleJSONResponse(response)
  } else if (contentType.includes('text/html')) {
    return handleTextResponse(response)
  } else {
    // Other response types as necessary. I haven't found a need for them yet though.
    throw new Error(`Sorry, content-type ${contentType} not supported`)
  }
}

function handleJSONResponse (response) {
  return response.json()
    .then(json => {
      console.log(JSON.stringify(json));

      if (response.ok) {
         console.log(`Line 46 companion - fetch - handleJSONResponse() response.ok`)
        return json
      } else {
        return Promise.reject(Object.assign({}, json, {
          status: response.status,
          statusText: response.statusText
        }))
      }
    })
}
// This doesnt work
function handleTextResponse (response) {
  return response.text()
    .then(text => {
      if (response.ok) {
        console.log(`Line 98 companion - fetch - handleTextResponse() response.ok`)
        return JSON.parse(text)
      } else {
        return Promise.reject({
          status: response.status,
          statusText: response.statusText,
          err: text
        })
      }
    })
  }