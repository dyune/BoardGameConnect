/**
 * Board Game Oracle API integration
 * This service provides functions to search and fetch board game data
 */

// BGG XML API as a fallback since Board Game Atlas is no longer available
const BASE_URL = 'https://boardgamegeek.com/xmlapi2';

// Cache for search results to minimize API calls
const searchCache = new Map();
const gameDetailsCache = new Map();

// Fallback data in case of CORS issues with BGG
const popularGames = [
  {
    id: "174430",
    name: "Gloomhaven",
    year_published: "2017",
    min_players: 1,
    max_players: 4,
    image_url: "https://cf.geekdo-images.com/sZYp_3BTDGjh2unaZfZmuA__original/img/7d-lj5Gd1e8PFnD97LYFah2c45M=/0x0/filters:format(jpeg)/pic2437871.jpg",
    description: "Vanquish monsters with strategic cardplay in a 95-scenario campaign.",
    categories: ["Adventure", "Exploration", "Fantasy", "Fighting"],
    publisher: "Cephalofair Games"
  },
  {
    id: "167791",
    name: "Terraforming Mars",
    year_published: "2016",
    min_players: 1,
    max_players: 5,
    image_url: "https://cf.geekdo-images.com/wg9oOLcsKvDesSUdZQ4rxw__original/img/thIqWDnH9utKuoKVEUqveDixprI=/0x0/filters:format(jpeg)/pic3536616.jpg",
    description: "Compete with rival CEOs to make Mars habitable and build your corporate empire.",
    categories: ["Economic", "Science Fiction", "Territory Building"],
    publisher: "FryxGames"
  },
  {
    id: "169786",
    name: "Scythe",
    year_published: "2016",
    min_players: 1,
    max_players: 5,
    image_url: "https://cf.geekdo-images.com/7k_nOxpO9OGIjhLq2BUZdA__original/img/R-0hj8eM0pWMElgECgwP2bRnApU=/0x0/filters:format(jpeg)/pic3163924.jpg",
    description: "Five factions vie for dominance in a war-torn, mech-filled, alternate 1920s Europe.",
    categories: ["Economic", "Fighting", "Science Fiction", "Territory Building"],
    publisher: "Stonemaier Games"
  },
  {
    id: "233078",
    name: "Wingspan",
    year_published: "2019",
    min_players: 1,
    max_players: 5,
    image_url: "https://cf.geekdo-images.com/yLZJCVLlIx4c7eJEWUNJ7w__original/img/cI782Zis9cT66j2MjSNO-1S0mGI=/0x0/filters:format(jpeg)/pic4458123.jpg",
    description: "Attract a beautiful and diverse collection of birds to your wildlife preserve.",
    categories: ["Animals", "Card Game", "Set Collection"],
    publisher: "Stonemaier Games"
  },
  {
    id: "224517",
    name: "Brass: Birmingham",
    year_published: "2018",
    min_players: 2,
    max_players: 4,
    image_url: "https://cf.geekdo-images.com/x3zxjr-Vw5iU4yDPg70Jgw__original/img/FpyxH41Y6_ROoePAilwLIpcLVgs=/0x0/filters:format(jpeg)/pic3490053.jpg",
    description: "Build networks, grow industries, and navigate the industrial revolution.",
    categories: ["Economic", "Industry / Manufacturing", "Transportation"],
    publisher: "Roxley"
  },
  {
    id: "266192",
    name: "Wingspan: European Expansion",
    year_published: "2019",
    min_players: 1,
    max_players: 5,
    image_url: "https://cf.geekdo-images.com/f_he0W6OS6cnSlKj2UOcbQ__original/img/_OcRq_mJiMhiV4dwh9Vkz_FdR3c=/0x0/filters:format(jpeg)/pic5023739.jpg",
    description: "New European birds and round-end abilities in Wingspan.",
    categories: ["Animals", "Card Game", "Expansion for Base-game"],
    publisher: "Stonemaier Games"
  },
  {
    id: "162886",
    name: "Spirit Island",
    year_published: "2017",
    min_players: 1,
    max_players: 4,
    image_url: "https://cf.geekdo-images.com/a13ieMPP2s0KEaKNYmtH5w__original/img/QyDF_XlZLJBpfslHdDQpJvJoC5I=/0x0/filters:format(jpeg)/pic3615739.png",
    description: "Island Spirits join forces to defend their home from colonizing invaders.",
    categories: ["Fantasy", "Fighting", "Territory Building"],
    publisher: "Greater Than Games"
  },
  {
    id: "220308",
    name: "Gaia Project",
    year_published: "2017",
    min_players: 1,
    max_players: 4,
    image_url: "https://cf.geekdo-images.com/hGWFm3hbMlCDsfCsauOQ4g__original/img/tgH_VEUPk-bxkOUOjRYvgaAJJMw=/0x0/filters:format(jpeg)/pic5375625.png",
    description: "Expand, research, upgrade, and settle the galaxy with one of 14 alien species.",
    categories: ["Civilization", "Science Fiction", "Territory Building"],
    publisher: "Feuerland Spiele"
  },
  {
    id: "237182",
    name: "Root",
    year_published: "2018",
    min_players: 2,
    max_players: 4,
    image_url: "https://cf.geekdo-images.com/JUAUWaWrWtkGisBTUdxc-g__original/img/8jkLu6DEfOvdKkJZNW92cZd3YNU=/0x0/filters:format(jpeg)/pic4254509.jpg",
    description: "Control one of four factions in a battle for the forest.",
    categories: ["Animals", "Fighting", "Territory Building", "Wargame"],
    publisher: "Leder Games"
  },
  {
    id: "173346",
    name: "7 Wonders Duel",
    year_published: "2015",
    min_players: 2,
    max_players: 2,
    image_url: "https://cf.geekdo-images.com/WzNs1mA_o22nRxNUbHyEFQ__original/img/j2xRmsBmYVUoch8iexvXRQlry1Y=/0x0/filters:format(jpeg)/pic3376065.jpg",
    description: "Science? Military? What path to victory will you pursue in this 2-player card drafting game?",
    categories: ["Ancient", "Card Game", "Civilization"],
    publisher: "Repos Production"
  }
];

// Flag to track if we've seen a CORS error
let experiencedCorsError = false;

/**
 * Helper function to parse XML responses
 * @param {string} xmlString - XML string to parse
 * @returns {Document} Parsed XML document
 */
const parseXML = (xmlString) => {
  const parser = new DOMParser();
  return parser.parseFromString(xmlString, 'text/xml');
};

/**
 * Convert XML game element to game object
 * @param {Element} gameElem - XML game element
 * @returns {Object} Game object with parsed data
 */
const convertXmlGameToObject = (gameElem) => {
  // Get the game ID
  const id = gameElem.getAttribute('id');
  
  // Get name - primary name is the one with type="primary"
  let name = '';
  const nameElements = gameElem.getElementsByTagName('name');
  for (let i = 0; i < nameElements.length; i++) {
    const nameElem = nameElements[i];
    if (nameElem.getAttribute('type') === 'primary') {
      name = nameElem.getAttribute('value');
      break;
    }
  }
  
  // Get year published
  const yearPublishedElem = gameElem.getElementsByTagName('yearpublished')[0];
  const yearPublished = yearPublishedElem ? yearPublishedElem.getAttribute('value') : '';
  
  // Get min/max players
  const minPlayersElem = gameElem.getElementsByTagName('minplayers')[0];
  const maxPlayersElem = gameElem.getElementsByTagName('maxplayers')[0];
  const minPlayers = minPlayersElem ? parseInt(minPlayersElem.getAttribute('value'), 10) : 1;
  const maxPlayers = maxPlayersElem ? parseInt(maxPlayersElem.getAttribute('value'), 10) : 1;
  
  // Get image
  const imageElem = gameElem.getElementsByTagName('image')[0];
  const thumbnailElem = gameElem.getElementsByTagName('thumbnail')[0];
  const image = imageElem ? imageElem.textContent : (thumbnailElem ? thumbnailElem.textContent : '');
  
  // Get description
  const descriptionElem = gameElem.getElementsByTagName('description')[0];
  const description = descriptionElem ? descriptionElem.textContent : '';
  
  // Get categories/mechanics
  const categories = [];
  const linkElements = gameElem.getElementsByTagName('link');
  for (let i = 0; i < linkElements.length; i++) {
    const linkElem = linkElements[i];
    if (linkElem.getAttribute('type') === 'boardgamecategory') {
      categories.push(linkElem.getAttribute('value'));
    }
  }
  
  // Get publisher
  let publisher = '';
  for (let i = 0; i < linkElements.length; i++) {
    const linkElem = linkElements[i];
    if (linkElem.getAttribute('type') === 'boardgamepublisher') {
      publisher = linkElem.getAttribute('value');
      break;
    }
  }
  
  return {
    id,
    name,
    yearPublished,
    minPlayers,
    maxPlayers,
    image,
    description,
    categories,
    publisher
  };
};

/**
 * Search for board games by name
 * @param {string} query - The game name to search for
 * @param {number} limit - Maximum number of results to return (default: 10)
 * @returns {Promise<Array>} Array of game objects
 */
export const searchBoardGames = async (query, limit = 10) => {
  if (!query || query.trim() === '') {
    return [];
  }
  
  // Check cache first
  const cacheKey = `${query.toLowerCase()}_${limit}`;
  if (searchCache.has(cacheKey)) {
    return searchCache.get(cacheKey);
  }
  
  // If we've already experienced CORS errors, use fallback data immediately
  if (experiencedCorsError) {
    console.log("Using fallback data due to previous CORS errors");
    const filteredGames = popularGames.filter(game => 
      game.name.toLowerCase().includes(query.toLowerCase())
    ).slice(0, limit);
    
    // Cache the results
    searchCache.set(cacheKey, filteredGames);
    return filteredGames;
  }

  try {
    // Use BoardGameGeek XML API
    const response = await fetch(`${BASE_URL}/search?query=${encodeURIComponent(query)}&type=boardgame`);
    
    if (!response.ok) {
      throw new Error(`BGG API error: ${response.status}`);
    }

    const xmlText = await response.text();
    const xmlDoc = parseXML(xmlText);
    
    // Process search results
    const items = xmlDoc.getElementsByTagName('item');
    const results = [];
    
    for (let i = 0; i < Math.min(items.length, limit); i++) {
      const item = items[i];
      const id = item.getAttribute('id');
      
      // Get the name (first name element)
      const nameElem = item.getElementsByTagName('name')[0];
      const name = nameElem ? nameElem.getAttribute('value') : '';
      
      // Get year published if available
      const yearElem = item.getElementsByTagName('yearpublished')[0];
      const yearPublished = yearElem ? yearElem.getAttribute('value') : '';
      
      results.push({
        id,
        name,
        year_published: yearPublished,
        // Add placeholder values that will be filled in when getting details
        min_players: null,
        max_players: null,
        description: '',
        image_url: '',
        categories: [],
        publisher: ''
      });
    }
    
    // Cache the results
    searchCache.set(cacheKey, results);
    return results;
  } catch (error) {
    console.error('Failed to search board games:', error);
    
    // If it's a CORS error, switch to fallback mode
    if (error.message.includes('CORS') || error.name === 'TypeError') {
      console.log("Switching to fallback data due to CORS error");
      experiencedCorsError = true;
      
      // Filter fallback data based on search query
      const filteredGames = popularGames.filter(game => 
        game.name.toLowerCase().includes(query.toLowerCase())
      ).slice(0, limit);
      
      // Cache the results
      searchCache.set(cacheKey, filteredGames);
      return filteredGames;
    }
    
    return [];
  }
};

/**
 * Get detailed information about a specific board game by ID
 * @param {string} gameId - The BoardGameGeek game ID
 * @returns {Promise<Object|null>} Game object or null if not found
 */
export const getBoardGameById = async (gameId) => {
  if (!gameId) {
    return null;
  }
  
  // Check cache first
  if (gameDetailsCache.has(gameId)) {
    return gameDetailsCache.get(gameId);
  }
  
  // Check if we have this game in our fallback data
  if (experiencedCorsError) {
    const fallbackGame = popularGames.find(game => game.id === gameId);
    if (fallbackGame) {
      // Format to match expected output structure
      const formattedGame = {
        id: fallbackGame.id,
        name: fallbackGame.name,
        yearPublished: fallbackGame.year_published,
        minPlayers: fallbackGame.min_players,
        maxPlayers: fallbackGame.max_players,
        image: fallbackGame.image_url,
        description: fallbackGame.description,
        categories: fallbackGame.categories,
        publisher: fallbackGame.publisher
      };
      
      // Cache the result
      gameDetailsCache.set(gameId, formattedGame);
      return formattedGame;
    }
  }

  try {
    const response = await fetch(`${BASE_URL}/thing?id=${gameId}&stats=1`);
    
    if (!response.ok) {
      throw new Error(`BGG API error: ${response.status}`);
    }

    const xmlText = await response.text();
    const xmlDoc = parseXML(xmlText);
    
    const items = xmlDoc.getElementsByTagName('item');
    if (items.length === 0) {
      return null;
    }
    
    const gameData = convertXmlGameToObject(items[0]);
    
    // Cache the results
    gameDetailsCache.set(gameId, gameData);
    return gameData;
  } catch (error) {
    console.error(`Failed to get board game with ID ${gameId}:`, error);
    
    // If it's a CORS error and we have fallback data, use it
    if ((error.message.includes('CORS') || error.name === 'TypeError') && !experiencedCorsError) {
      console.log("Switching to fallback data due to CORS error");
      experiencedCorsError = true;
      
      // Try again with the fallback approach
      return getBoardGameById(gameId);
    }
    
    return null;
  }
};

/**
 * Convert a BoardGameGeek game to the application's game format
 * @param {Object} bggGame - BoardGameGeek game object
 * @returns {Object} Formatted game object for the application
 */
export const convertBgaGameToAppFormat = (bggGame) => {
  if (!bggGame) return null;
  
  // Extract category names from array and join with commas
  let category = '';
  if (bggGame.categories && Array.isArray(bggGame.categories)) {
    category = bggGame.categories.join(', ');
  }
  
  return {
    name: bggGame.name,
    minPlayers: bggGame.minPlayers || bggGame.min_players || 1,
    maxPlayers: bggGame.maxPlayers || bggGame.max_players || 1,
    image: bggGame.image || bggGame.image_url || '',
    category: category,
    description: bggGame.description || '',
    publisher: bggGame.publisher || '',
    yearPublished: bggGame.yearPublished || bggGame.year_published || '',
    // Store the original BGG ID for reference
    bggId: bggGame.id
  };
}; 