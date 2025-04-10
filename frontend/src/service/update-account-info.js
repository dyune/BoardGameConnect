const BASE_URL = "http://localhost:8080/api/v1";
const TOKEN = localStorage.getItem("token");

export async function updateUsernamePassword(request) {
  try {
    fetch('http://localhost:8080/api/v1/account',
      { method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${TOKEN}`
        },
        body: JSON.stringify(request)
      }
    )
      .then(response => {
        if (!response.ok) {
          return response.text().then(errorText => {
            console.error('Error response:', errorText);
            // Handle the error text as needed
          });
        }
        return response.json(); // Use this if the response is valid JSON
      }
      ).catch(error => {
        console.error('Fetch error:', error);
      });
  }
  catch (e) {
    console.log(e);
  }
}

export async function upgradeAccountToGameOwner(email) {
  try {
    fetch(`${BASE_URL}/account/${email}`,
      {
        method: "PUT",
        credentials: "include",
        headers: {
          "Authorization": `Bearer ${TOKEN}`,
          "Content-Type": "application/json"
        },
      })
      .then(response => {
          if (response.ok) {
            return "Account upgraded to Game Owner, please refresh the page."
          }
          else {
            return "Account upgrade failed."
          }
      }
      ).catch(error => {
        console.error('Fetch error:', error);
    })

  }
  catch (e) {
    console.log(e);
  }
}
