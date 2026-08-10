import axiosClient from './axiosClient'

export const getTournaments = () => axiosClient.get('/api/tournaments')
export const getTournament = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}`)
export const getTournamentTeams = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/teams`)
export const getTournamentGroups = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/groups`)
export const getTournamentStandings = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/standings`)
export const getTournamentMatches = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/matches`)
export const getTournamentSummary = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/summary`)
export const getTournamentAwards = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/awards`)
export const getTournamentTeamAwards = (tournamentId) => axiosClient.get(`/api/tournaments/${tournamentId}/team-awards`)
