import React from 'react';
import { GameCard } from './GameCard';
import withAuthRestriction from '../common/withAuthRestriction';

// Create a version of GameCard that requires authentication to interact with it
const AuthRestrictedGameCard = withAuthRestriction(GameCard);

export default AuthRestrictedGameCard; 