import axios from 'axios';

const BASE_URL = '/api/manager';

export const getContracts = async () => {
    const response = await axios.get(`${BASE_URL}/contracts`);
    return response.data;
};

export const getExpiringContracts = async () => {
    const response = await axios.get(`${BASE_URL}/contracts/expiring`);
    return response.data;
};

export const getPlayerContracts = async (playerId) => {
    const response = await axios.get(`${BASE_URL}/players/${playerId}/contracts`);
    return response.data;
};

export const createContract = async (playerId) => {
    const response = await axios.post(`${BASE_URL}/players/${playerId}/contracts`);
    return response.data;
};

export const renewContract = async (contractId) => {
    const response = await axios.post(`${BASE_URL}/contracts/${contractId}/renew`);
    return response.data;
};

export const terminateContract = async (contractId) => {
    const response = await axios.post(`${BASE_URL}/contracts/${contractId}/terminate`);
    return response.data;
};

export const retirePlayer = async (playerId) => {
    const response = await axios.post(`${BASE_URL}/players/${playerId}/lifecycle/retire`);
    return response.data;
};

export const reactivatePlayer = async (playerId) => {
    const response = await axios.post(`${BASE_URL}/players/${playerId}/lifecycle/reactivate`);
    return response.data;
};
