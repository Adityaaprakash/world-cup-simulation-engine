import axiosClient from './axiosClient';

export const getScouts = async () => {
  const { data } = await axiosClient.get('/api/scouting/scouts');
  return data;
};

export const getReports = async () => {
  const { data } = await axiosClient.get('/api/scouting/reports');
  return data;
};

export const assignScout = async (scoutId, playerId) => {
  const { data } = await axiosClient.post('/api/scouting/assignments', {
    scoutId,
    playerId,
  });
  return data;
};

export const simulateAdvance = async (days) => {
  await axiosClient.post(`/api/scouting/simulate-advance?days=${days}`);
};
