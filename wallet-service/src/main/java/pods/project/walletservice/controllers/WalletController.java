package pods.project.walletservice.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pods.project.walletservice.entities.Wallet;
import pods.project.walletservice.repositories.WalletRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class WalletController {

    private static final Log log = LogFactory.getLog(WalletController.class);
    private final WalletRepository walletRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public WalletController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/wallets")
    public ResponseEntity<?> findAll() {
        List<Wallet> wallets = walletRepository.findAll();
        return ResponseEntity.ok().body(wallets);
    }

    @GetMapping("/wallets/{user_id}")
    public ResponseEntity<?> findByUserId(@PathVariable Integer user_id) {
        List<Wallet> wallets = walletRepository.findByUserId(user_id);
        if (wallets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(walletExistsNot(user_id));
        }
        return ResponseEntity.status(HttpStatus.OK).body(wallets.get(0));
    }

    @PostMapping("/wallets/{user_id}")
    public ResponseEntity<?> insertIntoWallets(@PathVariable Integer user_id) {
        boolean userExists = getUserById(user_id);
        if (!userExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userExistsNot(user_id));
        }

        List<Wallet> wallets = walletRepository.findByUserId(user_id);
        if (!wallets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletExists(user_id));
        }

        Wallet wallet = new Wallet();
        wallet.setBalance(0);
        wallet.setUser_id(user_id); /* this user must exist */
        walletRepository.save(wallet);
        return ResponseEntity.status(HttpStatus.OK).body(wallet);
    }

    @PutMapping("/wallets/{user_id}")
    public ResponseEntity<?> updateWalletBalance(@PathVariable Integer user_id, @RequestBody Map<String, String> wallet) {

        boolean userExists = getUserById(user_id);
        System.out.println("EXISTS: " + userExists);
        if (!userExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userExistsNot(user_id));
        }

        List<Wallet> wallets = walletRepository.findByUserId(user_id);
        if (wallets.isEmpty()) {
            Wallet wallet_ = new Wallet();
            wallet_.setBalance(0);
            wallet_.setUser_id(user_id); /* this user must exist */
            walletRepository.save(wallet_);
            wallets = walletRepository.findByUserId(user_id);
//            return ResponseEntity.status(HttpStatus.OK).body(wallet_);
        }

        boolean validPayload =  wallet != null && wallets.size() > 0;
        validPayload = validPayload && wallet.get("amount") != null && wallet.get("action") != null;
        validPayload = validPayload && !wallet.get("amount").isEmpty() && !wallet.get("action").isEmpty();
        System.out.println("VALID");
        if (!validPayload) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updateBalanceFailed("payload"));
        }

        Integer newBalance = null;
        try{
            Integer oldBalance = wallets.get(0).getBalance();
            newBalance = oldBalance;
            if(wallet.get("action").equals("credit")) {
                newBalance += Integer.parseInt(wallet.get("amount"));
            }else{
                newBalance -= Integer.parseInt(wallet.get("amount"));
            }
            if(newBalance < 0){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updateBalanceFailed("balance"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updateBalanceFailed("payload"));
        }
        System.out.println("UPDATE");
        Wallet wallet_ = new Wallet();
        wallet_.setBalance(newBalance);
        wallet_.setUser_id(user_id); /* this user must exist */
        walletRepository.save(wallet_);
        return ResponseEntity.status(HttpStatus.OK).body(wallet_);

    }

    @DeleteMapping("/wallets/{user_id}")
    public ResponseEntity<?> deleteWalletByUserId(@PathVariable Integer user_id) {
        List<Wallet> wallets = walletRepository.findByUserId(user_id);
        if (wallets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(walletExistsNot(user_id));
        }

        walletRepository.deleteById(user_id);
        /* the user has to be deleted too */
        /* all mappings have to be deleted */

        return ResponseEntity.status(HttpStatus.OK).body(walletDeletedSuccess(user_id));
    }

    @DeleteMapping("/wallets")
    public ResponseEntity<?> deleteWallets() {
        List<Wallet> wallets = walletRepository.findAll();
        if (wallets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(walletsExistNot());
        }

        walletRepository.deleteAll(); /* the user has to be deleted too */

        return ResponseEntity.status(HttpStatus.OK).body(walletsDeletedSuccessfully());
    }

    private Object walletsDeletedSuccessfully() {
        return "Wallets deleted successfully";
    }

    private Object walletsExistNot() {
        return "Wallets do NOT exist";
    }

    private Object walletDeletedSuccess(Integer user_id) {
        return "Wallet for User with id = " + user_id +" deleted successfully";
    }


    private String updateBalanceFailed(String type) {
        if(type.equals("balance")) {
            return "Wallet update failed (Debit/Credit Amount High/Low)";
        }
        return "Wallet balance update failed (Bad Payload request)";
    }

    private Object userExistsNot(Integer user_id) {
        return "User with id " + user_id + " does NOT exist";
    }

    public boolean getUserById2(Integer user_id) {
        String url = "http://localhost:8080/users/" + user_id;
        Map<Object, Object> map = null;
        try {
            map = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            return false;
        }
        return map != null && map.size() > 0;
    }

    public boolean getUserById(Integer user_id) {
        String url = "http://localhost:8080/users/" + user_id;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);

//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("userId", 123);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(null, null);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("User Found Successfully!");
                return true;
            }
            return false;
        } catch (RestClientException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private Object walletExists(Integer user_id) {
        return "Wallet for User with id = " + user_id + " already exist";
    }


    private Object walletExistsNot(Integer user_id) {
        return "Wallet for User with id = " + user_id + " does NOT exist";
    }

}
